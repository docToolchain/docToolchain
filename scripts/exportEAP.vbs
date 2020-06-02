    ' based on the "Project Interface Example" which comes with EA
    ' http://stackoverflow.com/questions/1441479/automated-method-to-export-enterprise-architect-diagrams

    Dim EAapp 'As EA.App
    Dim Repository 'As EA.Repository
    Dim FS 'As Scripting.FileSystemObject

    Dim projectInterface 'As EA.Project
    
    Const   ForAppending = 8
    
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

    Sub WriteNote(currentModel, currentElement, notes, prefix)
        If (Left(notes, 6) = "{adoc:") Then
            strFileName = Mid(notes,7,InStr(notes,"}")-7)
            strNotes = Right(notes,Len(notes)-InStr(notes,"}"))
            set objFSO = CreateObject("Scripting.FileSystemObject")
            If (currentModel.Name="Model") Then
              ' When we work with the default model, we don't need a sub directory
              path = "./src/docs/ea/"
            Else
              path = "./src/docs/ea/"&currentModel.Name&"/"
            End If
            MakeDir(path)
            ' WScript.echo path&strFileName
            post = ""
            If (prefix<>"") Then
                post = "_"
            End If
            MakeDir(path&prefix&post)
            set objFile = objFSO.OpenTextFile(path&prefix&post&strFileName&".ad",ForAppending, True)
            name = currentElement.Name
            name = Replace(name,vbCr,"")
            name = Replace(name,vbLf,"")
            ' WScript.echo "-"&Left(strNotes, 6)&"-"
            if (Left(strNotes, 3) = vbCRLF&"|") Then
                ' content should be rendered as table - so don't interfere with it
                objFile.WriteLine(vbCRLF)
            else
                'let's add the name of the object
                objFile.WriteLine(vbCRLF&vbCRLF&"."&name)
            End If
            objFile.WriteLine(vbCRLF&strNotes)
            objFile.Close
            if (prefix<>"") Then
                ' write the same to a second file
                set objFile = objFSO.OpenTextFile(path&prefix&".ad",ForAppending, True)
                objFile.WriteLine(vbCRLF&vbCRLF&"."&name&vbCRLF&strNotes)
                objFile.Close
            End If
        End If
    End Sub

    Sub SyncJira(currentModel, currentDiagram)
        notes = currentDiagram.notes
        set currentPackage = Repository.GetPackageByID(currentDiagram.PackageID)
        updated = 0
        created = 0
        If (Left(notes, 6) = "{jira:") Then
            WScript.echo " >>>> Diagram jira tag found"
            strSearch = Mid(notes,7,InStr(notes,"}")-7)
            Set objShell = CreateObject("WScript.Shell")
            'objShell.CurrentDirectory = fso.GetFolder("./scripts")
            Set objExecObject = objShell.Exec ("cmd /K  groovy ./scripts/exportJira.groovy """ & strSearch &""" & exit")
            strReturn = ""
            x = 0
            y = 0
            Do While Not objExecObject.StdOut.AtEndOfStream
                output = objExecObject.StdOut.ReadLine()
                ' WScript.echo output
                jiraElement = Split(output,"|")
                name = jiraElement(0)&":"&vbCR&vbLF&jiraElement(4)
                On Error Resume Next
                Set requirement = currentPackage.Elements.GetByName(name)
                On Error Goto 0
                if (IsObject(requirement)) then
                    ' element already exists
                    requirement.notes = ""
                    requirement.notes = requirement.notes&"<a href='"&jiraElement(5)&"'>"&jiraElement(0)&"</a>"&vbCR&vbLF
                    requirement.notes = requirement.notes&"Priority: "&jiraElement(1)&vbCR&vbLF
                    requirement.notes = requirement.notes&"Created: "&jiraElement(2)&vbCR&vbLF
                    requirement.notes = requirement.notes&"Assignee: "&jiraElement(3)&vbCR&vbLF
                    requirement.Update()
                    updated = updated + 1
                else
                    Set requirement = currentPackage.Elements.AddNew(name,"Requirement")
                    requirement.notes = ""
                    requirement.notes = requirement.notes&"<a href='"&jiraElement(5)&"'>"&jiraElement(0)&"</a>"&vbCR&vbLF
                    requirement.notes = requirement.notes&"Priority: "&jiraElement(1)&vbCR&vbLF
                    requirement.notes = requirement.notes&"Created: "&jiraElement(2)&vbCR&vbLF
                    requirement.notes = requirement.notes&"Assignee: "&jiraElement(3)&vbCR&vbLF
                    requirement.Update()
                    currentPackage.Elements.Refresh()
                    Set dia_obj = currentDiagram.DiagramObjects.AddNew("l="&(10+x*200)&";t="&(10+y*50)&";b="&(10+y*50+44)&";r="&(10+x*200+180),"")
                    x = x + 1
                    if (x>3) then
                      x = 0
                      y = y + 1
                    end if
                    dia_obj.ElementID = requirement.ElementID
                    dia_obj.Update()
                    created = created + 1
                end if
            Loop
            Set objShell = Nothing
            WScript.echo "created "&created&" requirements"
            WScript.echo "updated "&updated&" requirements"
        End If
    End Sub

    Sub SaveDiagram(currentModel, currentDiagram)
        ' Open the diagram
        Repository.OpenDiagram(currentDiagram.DiagramID)

        ' Save and close the diagram
        If (currentModel.Name="Model") Then
            ' When we work with the default model, we don't need a sub directory
            path = "/src/docs/images/ea/"
        Else
            path = "/src/docs/images/ea/" & currentModel.Name & "/"
        End If
        diagramName = Replace(currentDiagram.Name," ","_")
        diagramName = Replace(diagramName,vbCr,"")
        diagramName = Replace(diagramName,vbLf,"")
        filename = path & diagramName & ".png"
        MakeDir("." & path)
        projectInterface.SaveDiagramImageToFile(fso.GetAbsolutePathName(".")&filename)
        ' projectInterface.putDiagramImageToFile currentDiagram.DiagramID,fso.GetAbsolutePathName(".")&filename,1
        WScript.echo " extracted image to ." & filename
        Repository.CloseDiagram(currentDiagram.DiagramID)
        For Each diagramElement In currentDiagram.DiagramObjects
            Set currentElement = Repository.GetElementByID(diagramElement.ElementID)
            WriteNote currentModel, currentElement, currentElement.Notes, diagramName&"_notes"
        Next
        For Each diagramLink In currentDiagram.DiagramLinks
            set currentConnector = Repository.GetConnectorByID(diagramLink.ConnectorID)
            WriteNote currentModel, currentConnector, currentConnector.Notes, diagramName&"_links"
        Next
    End Sub
    '
    ' Recursively saves all diagrams under the provided package and its children
    '
    Sub DumpDiagrams(thePackage,currentModel)
        Set currentPackage = thePackage

        ' export element notes
        For Each currentElement In currentPackage.Elements
            WriteNote currentModel, currentElement, currentElement.Notes, ""
            ' export connector notes
            For Each currentConnector In currentElement.Connectors
                ' WScript.echo currentConnector.ConnectorGUID
                if (currentConnector.ClientID=currentElement.ElementID) Then
                    WriteNote currentModel, currentConnector, currentConnector.Notes, ""
                End If
            Next
            if (Not currentElement.CompositeDiagram Is Nothing) Then
                SyncJira currentModel, currentElement.CompositeDiagram
                SaveDiagram currentModel, currentElement.CompositeDiagram
            End If
            if (Not currentElement.Elements Is Nothing) Then
                DumpDiagrams currentElement,currentModel
            End If
        Next
        
        
        ' Iterate through all diagrams in the current package
        For Each currentDiagram In currentPackage.Diagrams
            SyncJira currentModel, currentDiagram
            SaveDiagram currentModel, currentDiagram
        Next

        ' Process child packages
        Dim childPackage 'as EA.Package
        ' otPackage = 5
        if (currentPackage.ObjectType = 5) Then
            For Each childPackage In currentPackage.Packages
                call DumpDiagrams(childPackage, currentModel)
            Next
        End If
    End Sub

    Function SearchEAProjects(path)
      For Each folder In path.SubFolders
        SearchEAProjects folder
      Next

      For Each file In path.Files
            If fso.GetExtensionName (file.Path) = "eap" OR fso.GetExtensionName (file.Path) = "eapx" Then
                WScript.echo "found "&file.path
                If (Left(file.name, 1) = "_") Then
                    WScript.echo "skipping, because it start with `_` (replication)"
                Else
                    OpenProject(file.Path)
                End If
            End If
      Next
    End Function

    'Gets the package object as referenced by its GUID from the Enterprise Architect project.
    'Looks for the model node, the package is a child of as it is required for the diagram export.
    'Calls the Sub routine DumpDiagrams for the model and package found.
    'An error is printed to console only if the packageGUID is not found in the project.
    Function DumpPackageDiagrams(EAapp, packageGUID)
        WScript.echo "DumpPackageDiagrams"
        WScript.echo packageGUID
        Dim package
        Set package = EAapp.Repository.GetPackageByGuid(packageGUID)
        If (package Is Nothing) Then
          WScript.echo "invalid package - as package is not part of the project"
        Else
          Dim currentModel
          Set currentModel = package
          while currentModel.IsModel = false
            Set currentModel = EAapp.Repository.GetPackageByID(currentModel.parentID)
          wend
          ' Iterate through all child packages and save out their diagrams
          ' save all diagrams of package itself
          call DumpDiagrams(package, currentModel)
        End If
    End Function

    Sub OpenProject(file)
      ' open Enterprise Architect
      Set EAapp = CreateObject("EA.App")
      WScript.echo "opening Enterprise Architect. This might take a moment..."
      ' load project
      EAapp.Repository.OpenFile(file)
      ' make Enterprise Architect to not appear on screen
      EAapp.Visible = False

      ' get repository object
      Set Repository = EAapp.Repository
      ' Show the script output window
      ' Repository.EnsureOutputVisible("Script")

      Set projectInterface = Repository.GetProjectInterface()

      Dim childPackage 'As EA.Package
      ' Iterate through all model nodes
      Dim currentModel 'As EA.Package
      If (InStrRev(file,"{") > 0) Then
        ' the filename references a GUID
        ' like {04C44F80-8DA1-4a6f-ECB8-982349872349}
        WScript.echo file
        GUID = Mid(file, InStrRev(file,"{")+0,38)
        WScript.echo GUID
        ' Iterate through all child packages and save out their diagrams
        call DumpPackageDiagrams(EAapp, GUID)
      Else
        If packageFilter.Count = 0 Then
          WScript.echo "done"
        ' Iterate through all model nodes
        For Each currentModel In Repository.Models
            ' Iterate through all child packages and save out their diagrams
            For Each childPackage In currentModel.Packages
                call DumpDiagrams(childPackage,currentModel)
            Next
        Next
        Else
          ' Iterate through all packages found in the package filter given by script parameter.
          For Each packageGUID In packageFilter
            call DumpPackageDiagrams(EAapp, packageGUID)
          Next
        End If
      End If
      EAapp.Repository.CloseFile()
    End Sub

  Private connectionString
  Private packageFilter
  Set packageFilter = CreateObject("System.Collections.ArrayList")
  Set objArguments = WScript.Arguments
  Dim argCount
  argCount = 0
  While objArguments.Count > argCount+1
    Select Case objArguments(argCount)
      Case "-c"
        connectionString = objArguments(argCount+1)
      Case "-p"
        packageFilter.Add objArguments(argCount+1)
    End Select
    argCount = argCount + 2
  WEnd
  set fso = CreateObject("Scripting.fileSystemObject") 
  WScript.echo "Image extractor"
  If IsEmpty(connectionString) Then
  WScript.echo "looking for .eap(x) files in " & fso.GetAbsolutePathName(".") & "/src"
  'Dim f As Scripting.Files
  SearchEAProjects fso.GetFolder("./src")
  Else
     WScript.echo "opening database connection now"
     OpenProject(connectionString)
  End If
  WScript.echo "finished exporting images"
