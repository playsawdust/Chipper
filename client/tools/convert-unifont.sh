#!/bin/bash

if [ -z "$1$2" ]; then
	echo "usage: $0 <unifont-bitmap> <output directory> [prefix]"
	echo "Converts a GNU Unifont atlas BMP to a Chipper font using ImageMagick and VIPS."
	echo "The directory will be created if it does not exist."
	echo "The input file is passed directly to ImageMagick, and as such, may be a URL."
	echo
	echo "e.g."
	echo "    $0 'http://unifoundry.com/pub/unifont/unifont-12.1.02/unifont-12.1.02.bmp' ../src/main/resources/res/sawdust/fonts/unifont"
	echo "    $0 'http://unifoundry.com/pub/unifont/unifont-12.1.02/unifont_plane1-12.1.02.bmp' ../src/main/resources/res/sawdust/fonts/unifont 1"
	exit 1
fi
tempdir=$(mktemp -d)
outdir=$(readlink -f "$2")
prefix=$3
cd "$tempdir"
convert -monitor "$1" \
	-gravity West -crop +32+64 +repage \
	 -negate -transparent Black \
	atlas.bmp
vips --vips-concurrency=$(nproc) --vips-progress dzsave atlas.bmp glyphs --depth one --tile-size 16 --overlap 0 --suffix .png
count=0
li=
echo
mkdir -p "$outdir"
for idx in $(seq 0 65535); do
	x=$(expr $idx % 256)
	y=$(expr $idx / 256)
	f=glyphs_files/0/${x}_${y}.png
	li="$li $f"
	if [ $(expr '(' $idx + 1 ')' % 256) == 0 ]; then
		block=$(printf "%02x" $(expr $idx / 256))
		montage -geometry 16x16+0+0 -tile 16x16 -background Transparent $li "$outdir/$prefix$block.png"
		li=
		count=$(expr $count + 1)
		echo -ne "\rbuilding block atlases... $count/256"
	fi
done
echo
rm -rf "$tempdir"
