'
' Script Name: Sort_Array
' Author: David Anderson
' Purpose: Sort a multi dimensional array
' Date: 3-Feb-2019
'
'==-----------------------------------------------------------==
'== This entire piece of code was shamelessly stolen from     ==
'==  the 4 Guys From Rolla WebWeekly newsletter, translated   ==
'==  to VBScript and changed into server-side ASP code.       ==
'== Every effort has been made to keep comments intact.       ==
'==                                                           ==
'== This version sorts 2-dimensional arrays on a single field ==
'==-----------------------------------------------------------==


Sub SwapRows(ary,row1,row2)
  '== This proc swaps two rows of an array 
  Dim x,tempvar
  For x = 0 to Ubound(ary,2)
    tempvar = ary(row1,x)    
    ary(row1,x) = ary(row2,x)
    ary(row2,x) = tempvar
  Next
End Sub  'SwapRows

Sub QuickSort(vec,loBound,hiBound,SortField)

  '==--------------------------------------------------------==
  '== Sort a 2 dimensional array on SortField                ==
  '==                                                        ==
  '== This procedure is adapted from the algorithm given in: ==
  '==    ~ Data Abstractions & Structures using C++ by ~     ==
  '==    ~ Mark Headington and David Riley, pg. 586    ~     ==
  '== Quicksort is the fastest array sorting routine for     ==
  '== unordered arrays.  Its big O is  n log n               ==
  '==                                                        ==
  '== Parameters:                                            ==
  '== vec       - array to be sorted                         ==
  '== SortField - The field to sort on (2nd dimension value) ==
  '== loBound and hiBound are simply the upper and lower     ==
  '==   bounds of the array's 1st dimension.  It's probably  ==
  '==   easiest to use the LBound and UBound functions to    ==
  '==   set these.                                           ==
  '==--------------------------------------------------------==

  Dim pivot(),loSwap,hiSwap,temp,counter
  Redim pivot (Ubound(vec,2))

  '== Two items to sort
  if hiBound - loBound = 1 then
    if vec(loBound,SortField) > vec(hiBound,SortField) then Call SwapRows(vec,hiBound,loBound)
  End If

  '== Three or more items to sort
  
  For counter = 0 to Ubound(vec,2)
    pivot(counter) = vec(int((loBound + hiBound) / 2),counter)
    vec(int((loBound + hiBound) / 2),counter) = vec(loBound,counter)
    vec(loBound,counter) = pivot(counter)
  Next

  loSwap = loBound + 1
  hiSwap = hiBound
  
  do
    '== Find the right loSwap
    while loSwap < hiSwap and vec(loSwap,SortField) <= pivot(SortField)
      loSwap = loSwap + 1
    wend
    '== Find the right hiSwap
    while vec(hiSwap,SortField) > pivot(SortField)
      hiSwap = hiSwap - 1
    wend
    '== Swap values if loSwap is less then hiSwap
    if loSwap < hiSwap then Call SwapRows(vec,loSwap,hiSwap)


  loop while loSwap < hiSwap
  
  For counter = 0 to Ubound(vec,2)
    vec(loBound,counter) = vec(hiSwap,counter)
    vec(hiSwap,counter) = pivot(counter)
  Next
    
  '== Recursively call function .. the beauty of Quicksort
    '== 2 or more items in first section
    if loBound < (hiSwap - 1) then Call QuickSort(vec,loBound,hiSwap-1,SortField)
    '== 2 or more items in second section
    if hiSwap + 1 < hibound then Call QuickSort(vec,hiSwap+1,hiBound,SortField)

End Sub  'QuickSort