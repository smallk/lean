#!/usr/bin/env sh

echo "*****************************************************"
echo "*                                                   *"
echo "*        Testing the custom twitter analyzer.       *"
echo "*                                                   *"
echo "*****************************************************"

java -jar DocIndexer/dist/DocIndexer.jar --config tests/test_twitter/test_twitter_config.yml
java -jar LuceneToMtx/dist/LuceneToMtx.jar --indir tests/test_twitter/index --outdir tests/test_twitter/results --minfreq 5

echo ""

if cmp -s tests/test_twitter/results/dictionary.txt tests/results_check/twitter/dictionary.txt; then
    echo "dictionary test passed"
else
    echo "dictionary test failed"
fi

if cmp -s tests/test_twitter/results/documents.txt tests/results_check/twitter/documents.txt; then
    echo "documents test passed"
else
    echo "documents test failed"
fi

if cmp -s tests/test_twitter/results/matrix.mtx tests/results_check/twitter/matrix.mtx; then
    echo "matrix test passed"
else
    echo "matrix test failed"
fi

rm -f -r tests/test_twitter/index
rm -f -r tests/test_twitter/results

echo ""

