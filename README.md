# lpexextensions
Lpex extensions for IBM Rational Developer for IBM i

This is a personal project to extend Lpex.

The first thing I'm trying is to convert fixed-format D-specifications to fully free.
This is quite rough at the moment because I am not a Java programmer by trade, 
so the Java could use quite a bit of refactoring.  I also need to stop and think
about writing Junit tests for it as well.

RDi classes as in query class.Directive
  alternatively, query elementClasses will return a list rather than 
  querying individual classes one at a time
CompileMessage
FullyFree
ConditionedOff
UndefineDirective
DefineDirective
EndIfDirective
ElseDirective
ElseIfDirective
IfDirective
EndDataStructure
StartDataStructure
DataStructure
DIRECTIVE
ISpec
OSPEC
CSpec
FSpec
PSpec
DSpec
HSpec
backwardLink
forwardLink
FixFormSQLEndReal
CFreeSQLEndReal
CFreeSQLEnd
CFreeSQLStart
CFreeSQL
FixFormSQLEnd
FixFormSQLStart
Fixed
Free
SUBROUTINE
SQL
SPACE
PROCEDURE
CONTROL
commentOnly
COMMENTS
Message

Notes to self:

/copy is Free DIRECTIVE

editor parameters
element - line number
