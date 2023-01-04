    ' based on the "Project Interface Example" which comes with EA
    ' http://stackoverflow.com/questions/1441479/automated-method-to-export-enterprise-architect-diagrams



    Dim EAapp 'As EA.App
    Dim Repository 'As EA.Repository
    Dim FS 'As Scripting.FileSystemObject

    Dim projectInterface 'As EA.Project

    Const   ForAppending = 8
    Const   ForWriting = 2

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

    ' Replaces certain characters with '_' to avoid unwanted file or folder names causing errors or structure failures.
    ' Regular expression can easily be extended with further characters to be replaced.
    Function NormalizeName(theName)
       dim re : Set re = new regexp
       re.Pattern = "[\\/\[\]\s]"
       re.Global = True
       NormalizeName = re.Replace(theName, "_")
    End Function

    Sub WriteNote(currentModel, currentElement, notes, prefix)
        If (Left(notes, 6) = "{adoc:") Then
            strFileName = Mid(notes,7,InStr(notes,"}")-7)
            strNotes = Right(notes,Len(notes)-InStr(notes,"}"))
            set objFSO = CreateObject("Scripting.FileSystemObject")
            If (currentModel.Name="Model") Then
              ' When we work with the default model, we don't need a sub directory
              path = objFSO.BuildPath(exportDestination,"ea/")
            Else
              path = objFSO.BuildPath(exportDestination,"ea/"&NormalizeName(currentModel.Name)&"/")
            End If
            MakeDir(path)

            post = ""
            If (prefix<>"") Then
                post = "_"
            End If
            MakeDir(path&prefix&post)

            set objFile = objFSO.OpenTextFile(path&prefix&post&strFileName&".ad",ForAppending, True)
            name = currentElement.Name
            name = Replace(name,vbCr,"")
            name = Replace(name,vbLf,"")

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
            Set objExecObject = objShell.Exec ("cmd /K  groovy ./scripts/exportEAPJiraPrintHelper.groovy """ & strSearch &""" & exit")
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

    ' This sub routine checks if the format string defined in diagramAttributes
    ' does contain any characters. It replaces the known placeholders:
    ' %DIAGRAM_AUTHOR%, %DIAGRAM_CREATED%, %DIAGRAM_GUID%, %DIAGRAM_MODIFIED%,
    ' %DIAGRAM_NAME%, %DIAGRAM_NOTES%, %DIAGRAM_DIAGRAM_TYPE%, %DIAGRAM_VERSION%
    ' with the attribute values read from the EA diagram object.
    ' None, one or multiple number of placeholders can be used to create a diagram attribute
    ' to be added to the document. The attribute string is stored as a file with the same
    ' path and name as the diagram image, but with suffix .ad. So, it can
    ' easily be included in an asciidoc file.
    Sub SaveDiagramAttribute(currentDiagram, path, diagramName)
        If Len(diagramAttributes) > 0 Then
            filledDiagAttr = diagramAttributes
            set objFSO = CreateObject("Scripting.FileSystemObject")
            filename = objFSO.BuildPath(path, diagramName & ".ad")
            set objFile = objFSO.OpenTextFile(filename, ForWriting, True)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_AUTHOR%",       currentDiagram.Author)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_CREATED%",      currentDiagram.CreatedDate)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_GUID%",         currentDiagram.DiagramGUID)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_MODIFIED%",     currentDiagram.ModifiedDate)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_NAME%",         currentDiagram.Name)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_NOTES%",        currentDiagram.Notes)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_DIAGRAM_TYPE%", currentDiagram.Type)
            filledDiagAttr = Replace(filledDiagAttr, "%DIAGRAM_VERSION%",      currentDiagram.Version)
            filledDiagAttr = Replace(filledDiagAttr, "%NEWLINE%",              vbCrLf)
            objFile.WriteLine(filledDiagAttr)
            objFile.Close
        End If
    End Sub
    Sub SaveDiagram(currentModel, currentDiagram)
        Dim exportDiagram ' As Boolean

        ' Open the diagram
        Repository.OpenDiagram(currentDiagram.DiagramID)

        ' Save and close the diagram
        set objFSO = CreateObject("Scripting.FileSystemObject")
        If (currentModel.Name="Model") Then
            ' When we work with the default model, we don't need a sub directory
            path = objFSO.BuildPath(exportDestination,"/images/ea/")
        Else
            path = objFSO.BuildPath(exportDestination,"/images/ea/" & NormalizeName(currentModel.Name) & "/")
        End If
        path = objFSO.GetAbsolutePathName(path)
        MakeDir(path)

        diagramName = currentDiagram.Name
        diagramName = Replace(diagramName,vbCr,"")
        diagramName = Replace(diagramName,vbLf,"")
        diagramName = NormalizeName(diagramName)
        filename = objFSO.BuildPath(path, diagramName & ".png")

        exportDiagram = True
        If objFSO.FileExists(filename) Then
            WScript.echo " --- " & filename & " already exists."
            If Len(additionalOptions) > 0 Then
                If InStr(additionalOptions, "KeepFirstDiagram") > 0 Then
                    WScript.echo " --- Skipping export -- parameter 'KeepFirstDiagram' set."
                Else
                    WScript.echo " --- Overwriting -- parameter 'KeepFirstDiagram' not set."
                    exportDiagram = False
                End If
            Else
                WScript.echo " --- Overwriting -- parameter 'KeepFirstDiagram' not set."
            End If
        End If
        If exportDiagram Then
          projectInterface.SaveDiagramImageToFile(filename)
          WScript.echo " extracted image to " & filename
          If Not IsEmpty(diagramAttributes) Then
              SaveDiagramAttribute currentDiagram, path, diagramName
          End If
        End If
        Repository.CloseDiagram(currentDiagram.DiagramID)

        ' Write the note of the diagram
        WriteNote currentModel, currentDiagram, currentDiagram.Notes, diagramName&"_notes"

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
            If fso.GetExtensionName (file.Path) = "eap" OR fso.GetExtensionName (file.Path) = "eapx" OR fso.GetExtensionName (file.Path) = "qea" OR fso.GetExtensionName (file.Path) = "qeax" Then
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

    Function FormatStringToJSONString(inputString)
        outputString = Replace(inputString, "\", "\\")
        outputString = Replace(outputString, """", "\""")
        outputString = Replace(outputString, vbCrLf, "\n")
        outputString = Replace(outputString, vbLf, "\n")
        outputString = Replace(outputString, vbCr, "\n")
        FormatStringToJSONString = outputString
    End Function

    'If a valid file path is set, the glossary terms are read from EA repository,
    'formatted in a JSON compatible format and written into file.
    'The file is read and reformatted by the exportEA gradle task afterwards.
    Function ExportGlossaryTermsAsJSONFile(EArepo)
        If (Len(glossaryFilePath) > 0) Then
            set objFSO = CreateObject("Scripting.FileSystemObject")
            GUID = Replace(EArepo.ProjectGUID,"{","")
            GUID = Replace(GUID,"}","")
            currentGlossaryFile = objFSO.BuildPath(glossaryFilePath,"/"&GUID&".ad")
            set objFile = objFSO.OpenTextFile(currentGlossaryFile,ForAppending, True)

            Set glossary = EArepo.Terms()
            objFile.WriteLine("[")
            dim counter
            counter = 0
            For Each term In glossary
                if (counter > 0) Then
                    objFile.Write(",")
                end if
                objFile.Write("{ ""term"" : """&FormatStringToJSONString(term.term)&""", ""meaning"" : """&FormatStringToJSONString(term.Meaning)&""",")
                objFile.WriteLine(" ""termID"" : """&FormatStringToJSONString(term.termID)&""", ""type"" : """&FormatStringToJSONString(term.type)&""" }")
                counter = counter + 1
            Next
            objFile.WriteLine("]")

            objFile.Close
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
      call ExportGlossaryTermsAsJSONFile(Repository)

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
      ' Since EA 15.2 the Enterprise Architect background process hangs without calling Exit explicitly
      EAapp.Repository.Exit()
    End Sub

  Private connectionString
  Private packageFilter
  Private exportDestination
  Private searchPath
  Private glossaryFilePath
  Private diagramAttributes
  Private additionalOptions

  exportDestination = "./src/docs"
  searchPath = "./src"
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
      Case "-d"
        exportDestination = objArguments(argCount+1)
      Case "-s"
        searchPath = objArguments(argCount+1)
      Case "-g"
        glossaryFilePath = objArguments(argCount+1)
      Case "-da"
        diagramAttributes = objArguments(argCount+1)
      Case "-ao"
        additionalOptions = objArguments(argCount+1)
    End Select
    argCount = argCount + 2
  WEnd
  set fso = CreateObject("Scripting.fileSystemObject")
  WScript.echo "Image extractor"

  ' Check both types in parallel - 1st check Enterprise Architect database connection, 2nd look for local project files
  If Not IsEmpty(connectionString) Then
     WScript.echo "opening database connection now"
     OpenProject(connectionString)
  End If
  WScript.echo "looking for .eap(x) files in " & fso.GetAbsolutePathName(searchPath)
  ' Dim f As Scripting.Files
  SearchEAProjects fso.GetFolder(searchPath)

  WScript.echo "finished exporting images"
