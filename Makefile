NXC=nbc
PROGRAM=segway
OUTPUT=segway

all: $(OUTPUT).rxe

$(OUTPUT).rxe: *.nxc Makefile
	$(NXC) -O=$(OUTPUT).rxe $(PROGRAM).nxc

down: $(OUTPUT).rxe
	$(NXC) -d $(OUTPUT).nxc

run: $(OUTPUT).rxe
	$(NXC) -r $(OUTPUT).nxc

clean:
	/bin/rm -v $(OUTPUT).rxe
