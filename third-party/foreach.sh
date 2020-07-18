#!/bin/bash
# foreach.sh
# (c) 2016-2019 Una Thompson
# Licensed under the CC0 Public Domain Dedication
# https://creativecommons.org/publicdomain/zero/1.0/
# (there, now i can carry this to any project i like without worrying about it)
for f in *; do
	if [[ -d "$f" && ! "$f" == *-ext-res ]]; then
		echo 'foreach.sh: changing directory to' $f
		cd "$f"
		echo 'foreach.sh: running' "$@"
		eval "$@"
		cd ..
		echo
		echo
	else
		echo 'foreach.sh: ignoring' $f
	fi
done
