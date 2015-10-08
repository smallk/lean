#!/usr/bin/env sh

echo "*****************************************************"
echo "*                                                   *"
echo "*             Testing the LEAN toolset.             *"
echo "*                                                   *"
echo "*****************************************************"

sh tests/test_docs/run_docs_test.sh | tee docs_test_results.txt
sh tests/test_mongo/run_mongo_test.sh | tee mongo_test_results.txt
sh tests/test_twitter/run_twitter_test.sh | tee twitter_test_results.txt

d="`grep -F failed docs_test_results.txt | wc -l`"
m="`grep -F failed mongo_test_results.txt | wc -l`"
t="`grep -F failed twitter_test_results.txt | wc -l`"

a="$(($d+$m+$t))"

if [ $a -ne 0 ]; then
	echo "*****************************************"
    echo "       ERROR: $a test(s) failed"
	echo "*****************************************"
else
	echo "*****************************************"
    echo "       SUCCESS: all tests passed"
	echo "*****************************************"
fi

rm -f -r docs_test_results.txt
rm -f -r mongo_test_results.txt
rm -f -r twitter_test_results.txt

