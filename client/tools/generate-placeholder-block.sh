#!/bin/bash

if [[ -z "$1" ]]; then
	echo "usage: $0 <output file>"
	echo "Generates a 256x256 Chipper font block with placeholder glyphs in Unifont style."
	echo "The block ID will be implied by the filename. It must be lowercase hex, followed by .png"
	echo "Uses ImageMagick."
	echo
	echo "e.g."
	echo "    $0 ../src/main/resources/res/sawdust/fonts/unifont/106e.png"
	exit 1
fi
basename=$(basename "$1" .png)
ourdir=$(dirname $(readlink -f "$0"))
outputfile=$(readlink -f "$1")
if [[ ${basename} =~ [^0-9a-f] ]]; then
	echo "$basename is not valid lowercase hex"
	exit 2
elif [[ ${basename:0:1} == "0" ]]; then
	echo "$basename cannot have trailing zeroes"
	exit 2
elif [[ ${#basename} -gt 4 ]]; then
	echo "$basename is outside of Unicode's range (00 through 1ff)"
	exit 3
elif [[ ${#basename} -lt 3 ]]; then
	echo "$basename is in the BMP; Unifont provides support for the entire BMP, and as such this script cannot generate placeholders for the BMP"
	exit 4
elif [[ ${#basename} -eq 4 && ${basename:0:1} != "1" ]]; then
	echo "$basename is outside of Unicode's range (00 through 1ff)"
	exit 3
fi
template=
if [[ ${#basename} -eq 4 ]]; then
	template="$ourdir/unifont_astral_example_block.png"
	basename=${basename:1}
else
	template="$ourdir/unifont_example_block.png"
fi
tempfile=$(mktemp --suffix=.png)
first=${basename:0:1}
second=${basename:1:1}
third=${basename:2:1}
convert \
	-size 16x16 xc:transparent \
	"$ourdir/block_name_chars/$first.png" \
	-geometry +3+2 -composite \
	"$ourdir/block_name_chars/$second.png" \
	-geometry +7+2 -composite \
	"$ourdir/block_name_chars/$third.png" \
	-geometry +11+2 -composite \
	-extent 16x16 \
	"$tempfile"
convert \
	-background Transparent \
	"$template" \
	-size 256x256 "tile:$tempfile" \
	-composite \
	"$outputfile"
rm "$tempfile"
