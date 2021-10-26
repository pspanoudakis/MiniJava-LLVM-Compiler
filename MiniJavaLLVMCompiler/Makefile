all: compile

compile:
	java -jar ../jtb132di.jar -te minijava.jj
	java -jar ../javacc5.jar minijava-jtb.jj
	javac Main.java

clean:
	rm -r visitor
	rm -r syntaxtree
	rm *.class
	rm Visitors/*.class
	rm ClassInformation/*.class
	rm SymbolTable/*.class
	rm Token*.java
	rm ParseException*.java
	rm MiniJavaParser*.java
	rm JavaCharStream.java
	rm minijava-jtb.jj
