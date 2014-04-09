NXC=/usr/bin/nbc
PROGRAM=segway
OUTPUT=segway

all: $(OUTPUT).rxe

$(OUTPUT).rxe: *.nxc Makefile
	$(NXC) -O=$(OUTPUT).rxe $(PROGRAM).nxc

down: $(OUTPUT).rxe
	$(NXC) -d $(OUTPUT).nxc

clean:
	/bin/rm -v $(OUTPUT).rxe
