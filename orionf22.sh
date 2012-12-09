cd ./src
javac *.java
mkdir ./results
echo 1.insert_test.txt
java DNAFile ./test_files/1.insert_test.txt 10 4096 > ./results/1.insert_test.txt
echo 2.remove_test2.txt
java DNAFile ./test_files/2.remove_test1.txt 10 4096 > ./results/2.remove_test1.txt
echo 3.remove_test2.txt
java DNAFile ./test_files/3.remove_test2.txt 10 4096 > ./results/3.remove_test2.txt
echo 4.search_test.txt
java DNAFile ./test_files/4.search_test.txt 10 4096 > ./results/4.search_test.txt
echo 5.format_test.text
java DNAFile ./test_files/5.format_test.txt 10 4096 > ./results/5.format_test.txt
echo 6.memory_test.txt
java DNAFile ./test_files/6.memory_test.txt 10 4096 > ./results/memory_test.txt