all: pdf

pdf:
	for file in $$(ls *tex | cut -d \. -f 1) ; do make $$file.pdf ; done

%.pdf: %.tex
	latexmk -pdf $<

clean:
	latexmk -C
