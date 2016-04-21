package cn.edu.uestc.algorithm.c45.myc45;

/** Created by LCJ on 2016-04-19.*/
public class TestMain {
    public static void main(String[] args)
    {
        C4_5 c45 = new C4_5("in.arff", "outlook");
        c45.buildC45Tree();
    }
}
