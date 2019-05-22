Sub PrintArray(vec,lo,hi)
  '==-----------------------------------------==
  '== Print out an array from the lo bound    ==
  '==  to the hi bound.  Highlight the column ==
  '==  whose number matches parm mark         ==
  '==-----------------------------------------==

  Dim i,j,row
  call LOGTrace( "PrintArray: lo(" &  lo & "),hi(" & hi & ")")

  For i = lo to hi
	row=""
	For j = 0 to Ubound(vec,2)
		if j=0 then
			row = vec(i,j) 
		else
			row = row & ":" & vec(i,j) 
		end if
	Next
    'call Session.Output(row)
	call LOGDebug (row)

  Next
  call LOGDebug( "end of array")
  
End Sub  'PrintArray