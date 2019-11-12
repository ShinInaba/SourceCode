/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jp.co.infitech.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author inaba
 */
public class SourceCode {

    private final String pathString;
    private final String characterCode;
    private final List<String> loadLines = new ArrayList<>();
    private final List<String> analyzeLines = new ArrayList<>();
    private int countEmptyLine = 0;
    private int countAnnotation = 0;
    private int countPackage = 0;
    private int countImport = 0;
    private int countRightCuryBracket = 0;
    private int countSourceLine = 0;
    
    private String packageName;
    private String className;
    private boolean existClass = false;

    private final Pattern patternString = Pattern.compile("\".*\"");
    private final Pattern patternCharacter = Pattern.compile("'.'");
    private final Pattern patternComment1 = Pattern.compile("/\\*\\*.*\\*/");
    private final Pattern patternComment2 = Pattern.compile("/\\*.*\\*/");
    private final Pattern patternComment3 = Pattern.compile(".*/\\*.*\\*/");
    private final Pattern patternComment4 = Pattern.compile("(.*)//.*");
    private final Pattern patternFor = Pattern.compile("for.*\\(.*;.*;.*\\).*");
    private final Pattern patternClass = Pattern.compile(".*\\s+class\\s+(.+)\\s+(implements|extends|\\s+).*");

    public SourceCode(String pathString, String characterCode) {
        this.pathString = pathString;
        this.characterCode = characterCode;
        loadSourceCode();
        analyzeSourceCode();
    }
    
    private void loadSourceCode() {
        Path path = Paths.get(pathString);
        try(BufferedReader bufferedReader = Files.newBufferedReader(path, Charset.forName(characterCode))) {
            for(String line; (line = bufferedReader.readLine()) != null; ) {
                loadLines.add(line);
            }
        }
        catch(IOException exception) {
            System.out.print(exception);
        }
    }
    
    private void analyzeSourceCode() {
        boolean inComments = false;
        for(String line : loadLines) {
            
            // 前後空白：削除
            String analyzeLine = line.trim();
            
            // 空行：除外
            if(analyzeLine.isEmpty() == true ) {
                countEmptyLine++;
                continue;
            }
            
            // 文字列：削除
            Matcher matcherString = patternString.matcher(analyzeLine);
            analyzeLine = matcherString.replaceAll("\"\"");
            
            // 文字：削除
            Matcher matcherCharacter = patternCharacter.matcher(analyzeLine);
            analyzeLine = matcherCharacter.replaceAll("''");
            
            // コメント：除外
            // //開始
            if(analyzeLine.startsWith("//") == true) {
                continue;
            }
            // /** */行：除外
            Matcher matcherComment1 = patternComment1.matcher(analyzeLine);
            if(matcherComment1.matches() == true) {
                continue;
            }
            // /* */行
            Matcher matcherComment2 = patternComment2.matcher(analyzeLine);
            if(matcherComment2.matches() == true) {
                continue;
            }
            // ～//行：削除
            Matcher matcherComment4 = patternComment4.matcher(analyzeLine);
            analyzeLine = matcherComment4.replaceAll("$1");
            analyzeLine = analyzeLine.trim();
            
            // /* */含む行
            Matcher matcherComment3 = patternComment3.matcher(analyzeLine);
            analyzeLine = matcherComment3.replaceAll("''");
            // 複数行コメントの除外
            if(inComments == true) {
                if(analyzeLine.endsWith("*/") == true) {
                    inComments = false;
                    continue;
                }
                continue;
            }
            if(analyzeLine.startsWith("/*") == true) {
                inComments = true;
                continue;
            }

            // アノテーション：除外
            if(analyzeLine.startsWith("@") == true) {
                countAnnotation++;
                continue;
            }

            // マルチ行の分割
            // ;
            // try-catach resourceも駄目か。
            // for内;を@に置換
            Matcher matcherFor = patternFor.matcher(analyzeLine);
            if(matcherFor.matches() == true) {
                analyzeLine = analyzeLine.replaceAll(";", "@");
            }
            var indexSemicolon = analyzeLine.indexOf(";");
            var indexLeftCuryBracket = analyzeLine.indexOf("\\{");
            var indexRightCuryBracket = analyzeLine.indexOf("}");
            if(indexSemicolon == -1 && indexLeftCuryBracket == -1 && indexRightCuryBracket == -1) {
                addAnalyzeLines(analyzeLine);
            }
            else if(indexSemicolon != -1 && indexSemicolon == analyzeLine.length() - 1) {
                addAnalyzeLines(analyzeLine);
            }
            else if(indexLeftCuryBracket != -1 && indexLeftCuryBracket == analyzeLine.length() - 1) {
                addAnalyzeLines(analyzeLine);
            }
            else if(indexRightCuryBracket != -1 && indexRightCuryBracket == analyzeLine.length() - 1) {
                addAnalyzeLines(analyzeLine);
                countRightCuryBracket++;
            }
            else if(indexSemicolon != -1 && indexSemicolon != analyzeLine.length() - 1) {
                var splitLines = analyzeLine.split(";");
                for(String splitLine : splitLines) {
                    if(splitLine.isEmpty() == true) {
                        continue;
                    }
                    addAnalyzeLines(splitLine + ";");
                }
            }
            else if(indexLeftCuryBracket != -1 && indexLeftCuryBracket != analyzeLine.length() - 1) {
                var splitLines = analyzeLine.split("\\{");
                for(String splitLine : splitLines) {
                    if(splitLine.isEmpty() == true) {
                        continue;
                    }
                    addAnalyzeLines(splitLine + "\\{");
                }
            }
            else if(indexRightCuryBracket != -1 && indexRightCuryBracket != analyzeLine.length() - 1) {
                var splitLines = analyzeLine.split("}");
                for(String splitLine : splitLines) {
                    addAnalyzeLines(splitLine + "}");
                    countRightCuryBracket++;
                }
            }
            
        }
    }
    
    private void addAnalyzeLines(String line) {
        if(line.startsWith("package ") == true) {
            countPackage++;
            packageName = line;
        }
        else if(line.startsWith("import ") == true) {
            countImport++;
        }
        else {
            var addLine = line.trim();
            if(existClass == false) {
                Matcher matcherClass = patternClass.matcher(addLine);
                if(matcherClass.matches() == true) {
                    className = matcherClass.group(1);
                    existClass = true;
                }
            }
            countSourceLine++;
            analyzeLines.add(addLine);
        }
    }

    public List<String> getLoadLines() {
        return loadLines;
    }

    public List<String> getAnalyzeLines() {
        return analyzeLines;
    }

    public int getCountEmptyLine() {
        return countEmptyLine;
    }

    public int getCountAnnotation() {
        return countAnnotation;
    }

    public int getCountPackage() {
        return countPackage;
    }

    public int getCountImport() {
        return countImport;
    }

    public int getCountRightCuryBracket() {
        return countRightCuryBracket;
    }

    public int getCountSourceLine() {
        return countSourceLine;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println(new File(".").getAbsoluteFile().getParent());
        String file = ".\\src\\main\\java\\jp\\co\\infitech\\java\\SourceCode.java";
        
        SourceCode sourceCode = new SourceCode(file, "UTF-8");
//        SourceCode sourceCode = new SourceCode(file, "SJIS");
        sourceCode.getAnalyzeLines().forEach(System.out::println);
        System.out.println(sourceCode.getPackageName());
        System.out.println(sourceCode.getClassName());
        System.out.println(sourceCode.getCountEmptyLine());
        System.out.println(sourceCode.getCountAnnotation());
        System.out.println(sourceCode.getCountPackage());
        System.out.println(sourceCode.getCountImport());
        System.out.println(sourceCode.getCountRightCuryBracket());
        System.out.println(sourceCode.getCountSourceLine());
    }
}
