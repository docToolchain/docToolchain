# Convert all pages in all visio files in the given directory to png files.
# A Visio windows might flash shortly.
# The converted png files are stored in the same directory
# The name of the png file is concatenated from the Visio file name and the page name.
# In addtion all the comments are stored in adoc files.
# If the Viso file is named "MyVisio.vsdx" and the page is called "FirstPage"
# the name of the png file will be "MyVisio-FirstPage.png" and the comment will
# be stored in "MyVisio-FirstPage.adoc".
# But for the name of the adoc files there is an alternative. It can be given in the first 
# line of the comment. If it is given in the comment it has to be given in curly brackes 
# with the prefix "adoc:", e.g. {adoc:MyCommentFile.adoc}
# Prerequisites: Viso and PowerShell has to be installed on the computer.
# Parameter: SourcePath where visio files can be found
# Example powershell VisoPageToPngConverter.ps1 -SourcePath c:\convertertest\

Param
(
    [Parameter(Mandatory=$true,ValueFromPipeline=$true,Position=0)]
    [Alias('p')][String]$SourcePath
)


If (!(Test-Path -Path $SourcePath))
{
    Write-Warning "The path ""$SourcePath"" does not exist or is not accessible, please input the correct path."
    Exit
}

# Extend the source path to get only Visio files of the given directory and not in subdircetories
If ($SourcePath.EndsWith("\"))
{
    $SourcePath  = "$SourcePath*" 
}
Else
{
    $SourcePath  = "$SourcePath\*" 
}

$VisioFiles = Get-ChildItem -Path $SourcePath -Include *.vsdx,*.vssx,*.vstx,*.vxdm,*.vssm,*.vstm,*.vsd,*.vdw,*.vss,*.vst

If(!($VisioFiles))
{
    Write-Warning "There are no Visio files in the path ""$SourcePath""."
    Exit
}

$VisioApp = New-Object -ComObject Visio.Application
$VisioApp.Visible = $false
    
# Extract the png from all the files in the folder
Foreach($File in $VisioFiles)
{
    $FilePath = $File.FullName
               $FileDirectory = $File.DirectoryName   # Get the folder containing the Visio file. Will be used to store the png and adoc files
    $FileBaseName = $File.BaseName    # Get the filename to be used as part of the name of the png and adoc files
    
    Try
    {
        $Document = $VisioApp.Documents.Open($FilePath)
        $Pages = $VisioApp.ActiveDocument.Pages
        Foreach($Page in $Pages)
        {
            # Create valid filenames for the png and adoc files
            $PngFileName = $Page.Name -replace '[:/\\*?|<>]','-'
            $PngFileName = "$FileBaseName-$PngFileName.png"
            $AdocFileName = $PngFileName.Replace(".png", ".adoc")

            #TODO: this needs better logic
            $Page.Export("$FileDirectory\images\visio\$PngFileName")
            
            $AllPageComments = ""
            ForEach($PageComment in $Page.Comments)
            {
                # Extract adoc filename from comment text if the syntax is valid
                # Remove the filename from the text and save the comment in a file with a valid name
                $EofStringIndex = $PageComment.Text.IndexOf(".adoc}")
                if ($PageComment.Text.StartsWith("{adoc") -And ($EofStringIndex -gt 6))
                {
                    $AdocFileName = $PageComment.Text.Substring(6, $EofStringIndex -1)
                    $AllPageComments += $PageComment.Text.Substring($EofStringIndex + 6)
                }
                else
                {
                    $AllPageComments += $PageComment.Text+"`n"
                }
            }
            If ($AllPageComments)
            {

                $AdocFileName = $AdocFileName -replace '[:/\\*?|<>]','-'
                #TODO: this needs better logic
                $stream = [System.IO.StreamWriter] "$FileDirectory\visio\$AdocFileName"
                $stream.WriteLine($AllPageComments)
                $stream.close()
            }                    
        }
        $Document.Close()
    }
    Catch
    {
        if ($Document)
        {
            $Document.Close()
        }
        Write-Warning "One or more visio page(s) in file ""$FilePath"" have been lost in this converting."
        Write-Warning "Error was: $_"
    }
}
$VisioApp.Quit()