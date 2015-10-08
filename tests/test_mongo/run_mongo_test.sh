
echo "*****************************************************"
echo "*                                                   *"
echo "*        Testing the MongoDB implementation.        *"
echo "*                                                   *"
echo "*****************************************************"


java -jar DocIndexer/dist/DocIndexer.jar --config tests/test_mongo/test_mongo_config.yml
java -jar LuceneToMtx/dist/LuceneToMtx.jar --indir tests/test_mongo/index --outdir tests/test_mongo/results --minfreq 5

echo ""

if cmp -s tests/test_mongo/results/dictionary.txt tests/results_check/mongo/dictionary.txt; then
    echo "dictionary test passed"
else
    echo "dictionary test failed"
fi

if cmp -s tests/test_mongo/results/documents.txt tests/results_check/mongo/documents.txt; then
    echo "documents test passed"
else
    echo "documents test failed"
fi

if cmp -s tests/test_mongo/results/matrix.mtx tests/results_check/mongo/matrix.mtx; then
    echo "matrix test passed"
else
    echo "matrix test failed"
fi

rm -f -r tests/test_mongo/index
rm -f -r tests/test_mongo/results

echo ""
