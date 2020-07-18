#!/bin/bash
if [ -z "$2" ]; then
	echo "usage: $0 <input> <output> [artist] [title]"
	echo "Convert the audio file in <input> into an Opus file with required Chipper music tagging data."
	echo "Optionally override the title and artist."
	exit 1
fi
if ! type ffmpeg ffprobe opusenc >/dev/null; then
	echo "FFmpeg and opusenc are required. Install them and try again."
	exit 1
fi
set -o noglob
input="$1"
output="$2"
probe=$(ffprobe -hide_banner -v error -show_format -print_format flat "$input")
durSec=$(echo "$probe" |grep 'format.duration' |cut -d'=' -f2 |sed 's/"//g')
if [ -n "$3" ]; then
	artist="$3"
else
	artist=$(echo "$probe" |grep 'format.tags.ARTIST' |cut -d'=' -f2 |sed 's/"//g')
fi
if [ -n "$4" ]; then
	title="$4"
else
	title=$(echo "$probe" |grep 'format.tags.TITLE' |cut -d'=' -f2 |sed 's/"//g')
fi
durSamples=$(echo "$durSec * 48000" | bc)
durSamples=${durSamples%.*}
ffmpeg -hide_banner -v error -i "$input" -vn -map 'a:0' -f wav - | opusenc --padding 0 --bitrate 64 --title "$title" --artist "$artist" --comment "CHIPPER_DURATION_IN_SAMPLES=$durSamples" - "$output"
