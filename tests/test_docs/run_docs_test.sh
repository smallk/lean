
echo "*****************************************************"
echo "*                                                   *"
echo "*  Testing the recursive directory implementation.  *"
echo "*                                                   *"
echo "*****************************************************"


java -jar DocIndexer/dist/DocIndexer.jar --config tests/test_docs/test_docs_config.yml
java -jar LuceneToMtx/dist/LuceneToMtx.jar --indir tests/test_docs/index --outdir tests/test_docs/results --minfreq 5

echo ""

if cmp -s tests/test_docs/results/dictionary.txt tests/results_check/docs/dictionary.txt; then
    echo "dictionary test passed"
else
    echo "dictionary test failed"
fi

if cmp -s tests/test_docs/results/documents.txt tests/results_check/docs/documents.txt; then
    echo "documents test passed"
else
    echo "documents test failed"
fi

if cmp -s tests/test_docs/results/matrix.mtx tests/results_check/docs/matrix.mtx; then
    echo "matrix test passed"
else
    echo "matrix test failed"
fi

rm -f -r tests/test_docs/index
rm -f -r tests/test_docs/results

echo ""