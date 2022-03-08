    Const   ForAppending = 8
    Const   ppPlaceholderBody = 2

    ' Helper
    ' http://windowsitpro.com/windows/jsi-tip-10441-how-can-vbscript-create-multiple-folders-path-mkdir-command
    Function MakeDir (strPath)
      Dim strParentPath, objFSO
      Set objFSO = CreateObject("Scripting.FileSystemObject")
      On Error Resume Next
      strParentPath = objFSO.GetParentFolderName(strPath)

      If Not objFSO.FolderExists(strParentPath) Then MakeDir strParentPath
      If Not objFSO.FolderExists(strPath) Then objFSO.CreateFolder strPath
      On Error Goto 0
      MakeDir = objFSO.FolderExists(strPath)

    End Function

	Function SearchPresentations(path)

		  For Each folder In path.SubFolders
		    SearchPresentations folder
		  Next

		  For Each file In path.Files
				If (Left(fso.GetExtensionName (file.Path), 3) = "ppt") OR (Left(fso.GetExtensionName (file.Path), 3) = "pps")  Then
					WScript.echo "found "&file.path
					ExportSlides(file.Path)
				End If
		  Next

    End Function

    Sub ExportSlides(sFile)
        Set objRegEx = CreateObject("VBScript.RegExp")
        objRegEx.Global = True
        objRegEx.IgnoreCase = True
        objRegEx.MultiLine = True
        ' "." doesn't work for multiline in vbs, "[\s,\S]" does...
        objRegEx.Pattern = "[\s,\S]*{adoc}"
        ' http://www.pptfaq.com/FAQ00481_Export_the_notes_text_of_a_presentation.htm
        strFileName = fso.GetFIle(sFile).Name
        Err.Clear
        Set oPPT = CreateObject("PowerPoint.Application")
        Set oPres = oPPT.Presentations.Open(sFile, True, False, False) ' Read Only, No Title, No Window
        On Error resume next
        Set oSlides = oPres.Slides
        WScript.echo "number slides: "&oSlides.Count
        strNotesText = ""
        strImagePath = "/images/ppt/" & strFileName & "/"
        MakeDir(searchPath & strImagePath)
        strNotesPath = "/ppt/"
        MakeDir(searchPath & strNotesPath)
        For Each oSl In oSlides
           strSlideName = oSl.Name
           'WScript.echo fso.GetAbsolutePathName(searchPath) & strImagePath & strSlideName & ".jpg"
           oSl.Export fso.GetAbsolutePathName(searchPath) & strImagePath & strSlideName & ".jpg", ".jpg"

            For Each oSh In oSl.NotesPage.Shapes
                If oSh.PlaceholderFormat.Type = ppPlaceholderBody  Then
                    If oSh.HasTextFrame Then
                        If oSh.TextFrame.HasText Then
                            strCurrentNotes = oSh.TextFrame.TextRange.Text
                            strCurrentNotes = Replace(strCurrentNotes,vbVerticalTab, vbCrLf)
                            strCurrentNotes = Replace(strCurrentNotes,"{slide}","image::ppt/"&strFileName&"/"&strSlideName&".jpg[]")
                            ' remove speaker notes before marker "{adoc}"
                            strCurrentNotes = objRegEx.Replace(strCurrentNotes,"")
                            strNotesText = strNotesText  & vbCrLf & strCurrentNotes & vbCrLf & vbCrLf
                        End If
                    End If
                End If
            Next
        Next
        ' WScript.echo fso.GetAbsolutePathName(".") & strNotesPath&""&strFileName&".ad"
        ' http://stackoverflow.com/questions/2524703/save-text-file-utf-8-encoded-with-vba

        Set fsT = CreateObject("ADODB.Stream")
        fsT.Type = 2 'Specify stream type - we want To save text/string data.
        fsT.Charset = "utf-8" 'Specify charset For the source text data.
        fsT.Open 'Open the stream And write binary data To the object
        fsT.WriteText "ifndef::imagesdir[:imagesdir: ../../images]"&vbCrLf&CStr(strNotesText)
        fsT.SaveToFile fso.GetAbsolutePathName(searchPath) & strNotesPath&""&strFileName&".ad", 2 'Save binary data To disk
        oPres.Close()
        oPPT.Quit()

        If Err.Number <> 0 Then
            WScript.Echo "Error: " & Err.Number
            WScript.Echo "Error (Hex): " & Hex(Err.Number)
            WScript.Echo "Source: " &  Err.Source
            WScript.Echo "Description: " &  Err.Description
            Err.Clear             ' Clear the Error
        End If
    End Sub

  set fso = CreateObject("Scripting.fileSystemObject")
  WScript.echo "Slide extractor"
    Set objArguments = WScript.Arguments

    Dim argCount
    argCount = 0
    While objArguments.Count > argCount+1
      Select Case objArguments(argCount)
        Case "-s"
          searchPath = objArguments(argCount+1)
      End Select
      argCount = argCount + 2
    WEnd

  WScript.echo "looking for .ppt files in " & fso.GetAbsolutePathName(searchPath)
  SearchPresentations fso.GetFolder(searchPath)
  WScript.echo "finished exporting slides"
