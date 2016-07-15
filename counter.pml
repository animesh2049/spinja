#define NOPROC 5
#define LC 20
int gc = 0;
int nrpr = 0;
int noproc = NOPROC;

proctype P() {
	int x = 1, temp;
	do
	:: x > LC -> break;
	:: else -> gc = gc + 1 ; x = x + 1;
	od;
	nrpr = nrpr + 1;
}

init{
	int x = 1;
	do
	:: x > NOPROC -> break;
	:: else -> x = x + 1; run P()
	od;

	(nrpr == NOPROC);
	assert(gc == LC*noproc);
}
