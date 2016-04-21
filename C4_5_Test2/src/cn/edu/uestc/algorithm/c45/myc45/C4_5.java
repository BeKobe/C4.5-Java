package cn.edu.uestc.algorithm.c45.myc45;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Created by LCJ on 2016-04-19.*/
class C4_5 {
    private List<ArrayList<String>> data = new ArrayList<>();   // 原始数据
    private List<String> attrNames = new ArrayList<>();         // 原始标签向量
    private List<ArrayList<String>> attrValues = new ArrayList<>();     // 原始标签值向量
    private String decisionName;        // 需要做决策的目标标签
    private BufferedOutputStream outputStream;

    /**
     * 读取.arff格式的文件, 获取原始数据、标签和标签值
     * @param filepath 文件路径
     * @param decisionName 需要做决策的目标标签
     * */
    C4_5(String filepath, String decisionName)
    {
        this.decisionName = decisionName;
        File file = new File(filepath);
        BufferedReader reader = null;
        String split = ",";
        try {
            reader = new BufferedReader(new FileReader(file));
            String str;
            boolean isData = false;
            while((str = reader.readLine()) != null){

                if(str.trim().length() == 0)
                    continue;
                if(str.trim().startsWith("@data")){
                    isData = true;
                    continue;
                }
                if(str.trim().startsWith("@attribute")){
                    String patternString = "@attribute\\s+([^\\s]+)\\s*\\{([^\\}]+)\\}";
                    Pattern pattern = Pattern.compile(patternString);
                    Matcher m = pattern.matcher(str);
                    if(m.find()){
                        attrNames.add(m.group(1));
                        ArrayList<String> values = new ArrayList<>();
                        String[] vals = m.group(2).split(split);
                        for(String val: vals)
                            values.add(val.trim());
                        attrValues.add(values);
                    }
                }else if(isData){
                    String[] vals = str.split(split);
                    ArrayList<String> record = new ArrayList<>(attrNames.size());
                    for(String val: vals)
                        record.add(val.trim());
                    data.add(record);
                }
            }
            System.out.println(data);
            System.out.println(attrNames);
            System.out.println(attrValues);
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 利用换底公式求n以2为底的对数
     * */
    private double getLog2_N(double n)
    {
        return (Math.log(n) / Math.log(2));
    }

    /**
     * 求信息熵
     * */
    private double getInfoEntropy(String attrName,
                                  List<ArrayList<String>> data,
                                  List<String> attrNames,
                                  List<ArrayList<String>> attrValues)
    {
        int index = attrNames.indexOf(attrName);
        int dataSize = data.size();
        double infoEntropy = 0.0;
        for(String value: attrValues.get(index))
        {
            int num = 0;
            for(ArrayList<String> dataItem: data)
            {
                if (dataItem.get(index).equals(value))
                {
                    num += 1;
                }
            }
            if (num != 0) {
                infoEntropy += -((double) num / (double) dataSize) * getLog2_N((double) num / (double) dataSize);
            }
        }
        return infoEntropy;
    }

    /**
     * 求信息增益
     * 公式: gain(X) = info(S) - info_x(S)
     * */
    private double getInfoGain(String attrName,
                               List<ArrayList<String>> data,
                               List<String> attrNames,
                               List<ArrayList<String>> attrValues)
    {
        int index = attrNames.indexOf(attrName);
        int dataSize = data.size();
        double infoEntropy_attrName_decisionName = 0.0;
        Map<String, Integer> attrNum = new HashMap<>();
        Map<List<String>, Integer> value_of_attr_decision = new HashMap<>();
        for (ArrayList<String> dataItem: data)
        {
            attrNum.put(dataItem.get(index), attrNum.getOrDefault(dataItem.get(index), 0) + 1);
            List<String> attr_decision = Arrays.asList(dataItem.get(index),
                    dataItem.get(attrNames.indexOf(decisionName)));
            value_of_attr_decision.put(attr_decision, value_of_attr_decision.getOrDefault(attr_decision, 0) + 1);
        }
        // System.out.println(attrNum);
        // System.out.println(value_of_attr_decision);
        for (Map.Entry<String, Integer> entry : attrNum.entrySet()) {
            Iterator<Map.Entry<List<String>, Integer>> temp = value_of_attr_decision.entrySet().iterator();
            double t1 = 0.0;
            while (temp.hasNext()) {
                Map.Entry<List<String>, Integer> entry1 = temp.next();
                if (entry1.getKey().get(0).equals(entry.getKey())) {
                    t1 += -((double) entry1.getValue() / (double) entry.getValue()) *
                            getLog2_N((double) entry1.getValue() / (double) entry.getValue());
                }
            }
            infoEntropy_attrName_decisionName += ((double) entry.getValue() / (double) dataSize) * t1;
        }
        return getInfoEntropy(decisionName, data, attrNames, attrValues) - infoEntropy_attrName_decisionName;
    }

    /**
     * 求信息增益率
     * */
    private double getGainRatio(String attrName, List<ArrayList<String>> data,
                                List<String> attrNames, List<ArrayList<String>> attrValues)
    {
        return getInfoGain(attrName, data, attrNames, attrValues) /
                getInfoEntropy(attrName, data, attrNames, attrValues);
    }

    /**
     * 获取具有最大信息增益率的标签
     * */
    private String getMaxGainRatioAttr(List<ArrayList<String>> data,
                               List<String> attrNames, List<ArrayList<String>> attrValues)
    {
        List<String> tempAttrNames = new ArrayList<>();
        tempAttrNames.addAll(attrNames);
        tempAttrNames.remove(decisionName);
        List<List<Object>> attrGainRatio = new ArrayList<>();
        for (String attr: tempAttrNames)
        {
            List<Object> temp = Arrays.asList(attr, getGainRatio(attr, data, attrNames, attrValues));
            attrGainRatio.add(temp);
        }
        Collections.sort(attrGainRatio,
                (o1, o2) -> (new Double((double)o2.get(1))).compareTo((double) o1.get(1)));
        // System.out.println(attrGainRatio);
        return (String)attrGainRatio.get(0).get(0);
    }

    /**
     * 获取具有最大信息增益率的标签后, 按此标签的标签值分裂数据
     * */
    private Map<String, List<ArrayList<String>>> getCutData(String attrName,
                                                            List<ArrayList<String>> data,
                                                            List<String> attrNames,
                                                            List<ArrayList<String>> attrValues)
    {
        int index = attrNames.indexOf(attrName);
        Map<String, List<ArrayList<String>>> cutData = new HashMap<>();
        for (String value : attrValues.get(index))
        {
            List<ArrayList<String>> newData = new ArrayList<>();
            data.stream().filter(dataItem -> dataItem.get(index).equals(value)).forEach(dataItem -> {
                ArrayList<String> temp = new ArrayList<>();
                temp.addAll(dataItem);
                temp.remove(value);
                newData.add(temp);
            });
            if (newData.size() > 0) {
                cutData.put(value, newData);
            }
        }
        return cutData;
    }

    /**
     * 获取具有最大信息增益率的标签后, 需要在原标签向量和标签值向量基础上去除这个具有最大信息增益率的标签和其标签值
     * */
    private void getCutAttrNames_AttrValues(List<String> attrNames, List<String> names,
                                            List<ArrayList<String>> attrValues, List<ArrayList<String>> values,
                                            int i)
    {
        names.addAll(attrNames);
        names.remove(i);
        values.addAll(attrValues);
        values.remove(i);
    }

    /**
     * 判断算法是否收敛的标志, 即数据中需要决策的目标标签的数据种类是否只有一种
     * */
    private boolean isDecisionValuesSettled(List<ArrayList<String>> data, List<String> attrNames)
    {
        int index = attrNames.indexOf(decisionName);
        Set<String> result = data.stream().map(dataItem -> dataItem.get(index)).collect(Collectors.toSet());
        return result.size() == 1;
    }

    /**
     * 递归构建C4.5决策树
     * 同时将决策结果写入文本文件
     * */
    private void buildTree(List<ArrayList<String>> data,
                           List<String> attrNames, List<ArrayList<String>> attrValues) throws IOException
    {
        String attr = getMaxGainRatioAttr(data, attrNames, attrValues);
        Map<String, List<ArrayList<String>>> cutData = getCutData(attr, data, attrNames, attrValues);
        for (Map.Entry<String, List<ArrayList<String>>> entry : cutData.entrySet()) {
            System.out.println(attr + "=" + entry.getKey() + " : " + entry.getValue());
            String spaces = "";
            for (int i = 0; i < ((this.data.get(0).size() - 1) - entry.getValue().get(0).size()); i++) {
                spaces += "\t";
            }
            outputStream.write((spaces + attr + "=" + entry.getKey()).getBytes());
            int index = attrNames.indexOf(attr);
            List<String> names = new ArrayList<>();
            List<ArrayList<String>> values = new ArrayList<>();
            getCutAttrNames_AttrValues(attrNames, names, attrValues, values, index);
            if ((!isDecisionValuesSettled(entry.getValue(), names)) && (names.size() > 1))
            {
                outputStream.write("\n".getBytes());
                buildTree(entry.getValue(), names, values);
            }
            else
            {
                outputStream.write((" : " +
                        entry.getValue().get(0).get(names.indexOf(decisionName)) +
                        "\n").getBytes());
            }
        }
    }

    /**
     * 调用构建决策树的函数来完成构建和写入文本的工作
     * */
    void buildC45Tree()
    {
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream("out.txt"));
            buildTree(data, attrNames, attrValues);
            outputStream.flush();
            outputStream.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
