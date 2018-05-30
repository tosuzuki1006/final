package jp.co.goalist.tech;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class techfinal {

    public static void main(String[] args) {

        System.out.println("作家の一文");
        try {

            // ランキングページををクローリングして、（作品名、作品本文が掲載されているURL）マップをつくる①
            Map<String, String> urlMap = makeUrlMap();

            // ①の動作確認用
            // kakunin1(urlMap);

            // urlマップを回しながら本文をクローリングして、（本文,<作品名,作者,URL>）のマップをつくる②

            Map<String, List<String>> nakamiMap = makeNakamiMap(urlMap);

            // ②の動作確認用(OK!)
            // kakunin2(nakamiMap);

            // <一文、<作品名,作者,URL>>マップ作る③
            Map<String, List<String>> ichibunMap = makeIchibunMap(nakamiMap);

            // ③の動作確認
            // kakunin3(nakamiMap);

            // とても適当な概念マスタ
            String[] gainenData = { "愛", "恋", "死", "神", "罪", "罰", "善", "夢", "美", "老", "病", "金", "男", "女" };
            // 概念的な言葉が含まれていたら（一つの文、概念）マップに格納④
            Map<String, List<String>> gainenMap = makeGainenMap(ichibunMap, gainenData);

            // ④動作確認
            // kakunin4(gainenMap);

            // 概念順にならべてcsvファイルに書き出し⑤

            // writeCsv(ichibunMap, gainenMap, gainenData);

            // htmlをつくったるで⑥
            writeHtml(ichibunMap, gainenMap, gainenData);

            System.out.println("おわった");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> makeUrlMap() throws IOException {

        // ランキングページををクローリングして、（作品名、作品本文が掲載されているURL）マップをつくる
        Map<String, String> urlMap = new LinkedHashMap<String, String>();

        try {
            String rootUrl = "https://www.aozora.gr.jp/access_ranking/2017_05_xhtml.html"; // 2017年ランキング
            Document doc = Jsoup.connect(rootUrl).get(); // ページの内容を要求し、その内容をDocument型のdocとして取り回していく
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Elements tr = doc.getElementsByTag("tr");// テーブル内の要素をすべて取得

            int cnt = 0;
            for (Element top : tr) {
                cnt++;
                // ヘッダはスキップ
                if (cnt == 1) {
                    continue;
                }

                // 今回は4位まで
                if (cnt == 5) {
                    break;
                }

                String name = top.child(1).text();
                String detailUrl = top.child(1).child(0).attr("href");// ここでは作品詳細ページを格納（詳細ページの中に本文リンクがある）

                String rootDetailUrl = detailUrl; // 詳細ページを指定
                Document detailDoc = Jsoup.connect(rootDetailUrl).get();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Element xhtml = detailDoc.select("body > div:nth-child(6) > a:nth-child(2)").get(0);// 本文URLがある場所
                String workUrl = xhtml.attr("href");
                String finalWorkUrl = detailUrl.substring(0, detailUrl.lastIndexOf("/")) + workUrl.substring(1);

                urlMap.put(name, finalWorkUrl);
            }
        } catch (IOException e) {
            throw e;
        }

        return urlMap;
    }

    private static void kakunin1(Map<String, String> urlMap) {
        for (Map.Entry<String, String> bar : urlMap.entrySet()) {
            String title = bar.getKey();
            String url = bar.getValue();
            System.out.println(title + "," + url);
            System.out.println();
        }
    }

    private static Map<String, List<String>> makeNakamiMap(Map<String, String> urlMap) throws IOException {

        // urlマップを回しながら本文をクローリングして、（本文,<作品名,作者,URL>）のマップをつくる
        Map<String, List<String>> nakamiMap = new LinkedHashMap<String, List<String>>();

        try {
            // Urlマップを回す
            for (Map.Entry<String, String> url : urlMap.entrySet()) {
                String workUrl = url.getValue();// 本文があるURL

                Document workDoc = Jsoup.connect(workUrl).get();

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // リストの情報からつくる
                Element titles = workDoc.getElementsByClass("title").get(0);
                Element authors = workDoc.getElementsByClass("author").get(0);
                List<String> infoList = new ArrayList<String>();

                String title = titles.text();
                String author = authors.text();

                infoList.add(title);
                infoList.add(author);
                infoList.add(workUrl);

                // 本文を取得して（本文,<作品名,作者,URL>）のマップをつくる
                Element works = workDoc.getElementsByClass("main_text").first();

                String work = works.text();

                // 本文を加工しやすいように微調整
                work = work.replaceAll("\\u0020", ",");// 半角スペースは,に置き換え
                work = work.replaceAll("\\u00A0", "");// 空白はなくす
                work = work.replaceAll("「", "");// カギかっこもなくす
                work = work.replaceAll("」", "");// カギかっこもなくす
                work = work.replaceAll("。", "。,");// 。のうしろにコンマつける
                nakamiMap.put(work, infoList);
            }
        } catch (IOException e) {
            throw e;
        }

        return nakamiMap;
    }

    private static void kakunin2(Map<String, List<String>> nakamiMap) throws IOException {
        Path answerPath = Paths.get("C:\\\\TechTraining\\\\resources\\\\finalCsv.csv");// 書き込み対象ファイルの場所を指定
        Files.deleteIfExists(answerPath);// もしあったらファイルを削除
        Files.createFile(answerPath);// ファイル作成
        try (BufferedWriter bw = Files.newBufferedWriter(answerPath)) {// １行ごとに「書き込む」

            for (Map.Entry<String, List<String>> bar : nakamiMap.entrySet()) {
                String honbun = bar.getKey();
                List<String> infoList = bar.getValue();
                String title = infoList.get(0);
                String author = infoList.get(1);
                String url = infoList.get(2);

                bw.write(honbun + "," + title + "," + author + "," + url);
                bw.newLine();
            }

        } catch (IOException e) {
            throw e;
        }
    }

    private static Map<String, List<String>> makeIchibunMap(Map<String, List<String>> nakamiMap) throws IOException {

        // 中身マップを回しながら本文を切って（一文,<作品名,作者,URL>）のマップをつくる
        Map<String, List<String>> ichibunMap = new LinkedHashMap<String, List<String>>();

        // 中身マップを回す
        for (Map.Entry<String, List<String>> honbun : nakamiMap.entrySet()) {
            String zenbun = honbun.getKey();// 全文
            List<String> infoList = honbun.getValue();// もろもろ情報

            // String[] cols = zenbun.split("/");

            // ?と。があったら切る
            String[] cols = zenbun.split(",");
            for (int i = 0; i < cols.length; i++) {
                String ichibun = cols[i];
                ichibunMap.put(ichibun, infoList);
            }
        }

        return ichibunMap;
    }

    private static void kakunin3(Map<String, List<String>> ichibunMap) throws IOException {
        Path answerPath = Paths.get("C:\\\\TechTraining\\\\resources\\\\finalCsv.csv");// 書き込み対象ファイルの場所を指定
        Files.deleteIfExists(answerPath);// もしあったらファイルを削除
        Files.createFile(answerPath);// ファイル作成
        try (BufferedWriter bw = Files.newBufferedWriter(answerPath)) {// １行ごとに「書き込む」

            for (Map.Entry<String, List<String>> bar : ichibunMap.entrySet()) {
                String ichibun = bar.getKey();
                List<String> infoList = bar.getValue();
                String title = infoList.get(0);
                String author = infoList.get(1);
                String url = infoList.get(2);

                bw.write(ichibun + "," + title + "," + author + "," + url);
                bw.newLine();
            }

        } catch (IOException e) {
            throw e;
        }
    }

    private static Map<String, List<String>> makeGainenMap(Map<String, List<String>> ichibunMap, String[] gainenData)
            throws IOException {

        // 一文マップを回しながら一文を見て行って、（一文,概念）のマップをつくる
        Map<String, List<String>> gainenMap = new LinkedHashMap<String, List<String>>();

        // 中身マップを回す
        for (Map.Entry<String, List<String>> bun : ichibunMap.entrySet()) {
            String ichibun = bun.getKey();// 一文
            // 一文に含まれる概念を格納するリスト
            List<String> gainenList = new ArrayList<String>();

            // 概念マスタを見比べながら、一文に概念が含まれていたらリストにぽいする
            for (int i = 0; i < gainenData.length; i++) {
                String gainen = gainenData[i];
                if (ichibun.contains(gainen)) {
                    gainenList.add(gainen);
                }
            }
            gainenMap.put(ichibun, gainenList);
        }
        return gainenMap;

    }

    private static void kakunin4(Map<String, List<String>> gainenMap) throws IOException {
        Path answerPath = Paths.get("C:\\\\TechTraining\\\\resources\\\\finalCsv.csv");// 書き込み対象ファイルの場所を指定
        Files.deleteIfExists(answerPath);// もしあったらファイルを削除
        Files.createFile(answerPath);// ファイル作成
        try (BufferedWriter bw = Files.newBufferedWriter(answerPath)) {// １行ごとに「書き込む」

            for (Map.Entry<String, List<String>> bar : gainenMap.entrySet()) {
                String ichibun = bar.getKey();
                List<String> infoList = bar.getValue();
                Iterator<String> gainenList = infoList.iterator();

                bw.write(ichibun);
                while (gainenList.hasNext()) {
                    String gainen = (String) gainenList.next();

                    bw.write("," + gainen);
                }

                bw.newLine();
            }

        } catch (IOException e) {
            throw e;
        }
    }

    private static void writeCsv(Map<String, List<String>> ichibunMap, Map<String, List<String>> gainenMap,
            String[] gainenData) throws IOException {

        try {
            Path answerPath = Paths.get("C:\\\\TechTraining\\\\resources\\\\finalCsv.csv");// 書き込み対象ファイルの場所を指定
            Files.deleteIfExists(answerPath);// もしあったらファイルを削除
            Files.createFile(answerPath);// ファイル作成
            try (BufferedWriter bw = Files.newBufferedWriter(answerPath)) {// １行ごとに「書き込む」
                for (int i = 0; i < gainenData.length; i++) {// 概念マスタを回して
                    String gainen = gainenData[i];
                    bw.write(gainen);
                    bw.newLine();
                    for (Map.Entry<String, List<String>> gainens : gainenMap.entrySet()) {// 概念マップをまわして
                        String ichibun = gainens.getKey();
                        List<String> gainenList = gainens.getValue();
                        List<String> infoList = ichibunMap.get(ichibun);// <作品名,作者,URL>
                        String name = infoList.get(0);
                        String author = infoList.get(1);
                        String URL = infoList.get(2);
                        if (gainenList.contains(gainen)) {// もしも概念がふくまれていたら

                            bw.write("・" + ichibun + "," + name + "," + author + "," + URL);// 書き込み
                            bw.newLine();
                        }

                    }
                    bw.newLine();

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw e;
        }
    }

    private static void writeHtml(Map<String, List<String>> ichibunMap, Map<String, List<String>> gainenMap,
            String[] gainenData) throws IOException {

        try {
            Path answerPath = Paths.get("C:\\\\TechTraining\\\\resources\\\\index.html");// 書き込み対象ファイルの場所を指定
            Files.deleteIfExists(answerPath);// もしあったらファイルを削除
            Files.createFile(answerPath);// ファイル作成
            try (BufferedWriter bw = Files.newBufferedWriter(answerPath)) {// １行ごとに「書き込む」

                // ヘッダーつくる

                bw.write("<!DOCTYPE html>");
                bw.write("<html>");
                bw.write("<head>");
                bw.write("<html lang=\"ja\">");
                bw.write("<meta charset=\"UTF-8\">");

                // css埋め込み
                bw.write("<style type=\"text/css\">");
                bw.write("a:link { color:#909090; text-decoration:none; font-size: 3em;}");
                bw.write("a:visited { color:#909090; text-decoration:none; font-size: 3em;}");
                bw.write("a:hover { color:#909090; text-decoration:none; font-size: 3em;}");
                bw.write("a:active { color:#909090; text-decoration:none; font-size: 3em;}");
                
                
                bw.write("li {");
                bw.write("font-family: 'Yu Mincho Light','YuMincho','Yu Mincho','游明朝体',sans-serif;\\");
                bw.write("font-size: 5em;");
                bw.write("color: #808080;");
                bw.write("letter-spacing: 0.05em;");
                bw.write("}");
                
                bw.write("h1 {");
                bw.write("font-family: 'Yu Mincho Light','YuMincho','Yu Mincho','游明朝体',sans-serif;\\");
                bw.write("font-size: 17.25em;");
                bw.write("color: #000000;");
                bw.write("}");
                bw.write("</style>");
                
                // JQuery読み込み
                bw.write("<script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js\"></script>");
                // javaScriptも中で書き込み
                bw.write("<script type=\"text/javascript\">");

                bw.write("$(function(){");// jQueryのはじまりだから消しちゃあかん

                bw.write("$('#btn').hover(function() {");

                bw.write(" $('#top').hide();");// トップは消えてなくなる
                // submitとclickイベントを同時にやるとあれだぞ！

                bw.write(" });");// マウスリーブファンクションの終わり

                bw.write("$('#btn').click(function(){");// クリックしたときに

                bw.write("var searchText = $('#id_textBox1').val();");// 入力された値
                // 検索窓の値を呼び出して

                bw.write("if(searchText!==\"\"){");// 検索窓がうまっていたら
                bw.write("$('#searshresult').attr('style', '');");

                // bw.write("$('#searchtitle').attr('style', '');");
                // bw.write("$('#searshresult').removeAttr('class');");//searshresultのhtmlを書き換え

                bw.write("$('.search-list li').each(function() {");// こいつらを変化させるぜ！

                bw.write("targetText = $(this).text();");// それぞれの値を呼び出して

                bw.write("if(targetText.indexOf(searchText) > -1){");

                bw.write("$(this).show();");

                bw.write("} else{");

                bw.write("$(this).hide();");
                bw.write("}");// 入れ子のif文のおわり

                bw.write("});");// イーチファンクションのおわり

                bw.write("} else{");// 検索窓がうまっていなかったら

                bw.write("alert('検索ワードを入力してください');");// 検索ワードを入力してください

                // bw.write("alert(searchText);");
                bw.write("}");// if文のおわり
                bw.write("});");// クリックファンクションのおわり

                bw.write("});");// jQueryのおわりだから消しちゃあかん

                bw.write("</script>");

                bw.write("<title>作家の一文</title>");
                bw.write("</head>");
                bw.write("<body>");

                // 検索エリア
                bw.write("<div class=\"search-area\">");

                // 検索フォーム
                bw.write("<form name=\"form1\" id=\"id_form1\" url=\"\" method=\"\">");
                bw.write("<input type=\"text\" name=\"textBox1\" id=\"id_textBox1\" placeholder=\"検索ワードを入力\">");
                bw.write("<input type=\"button\" id =\"btn\" value=\"検索\">");
                bw.write("</form>");

                // 検索結果表示場所
                // いったんぜんぶ出力して、jqueryで絞り込み
                bw.write("<div id=\"searshresult\" style=\"display:none;\">");
                // bw.write("<div id=\"searshresult\"<!--style=\"display:none;\" -->
                // >");//あとでcssいれてね！！！
                // bw.write("<h3 id =\"searchtitle\">検索結果</h3><br>");
                bw.write("<h3 id =\"searchtitle\">検索結果</h3><br>");
                // 一文はリスト化
                bw.write("<ul class=\"search-list\" style=\"list-style: none;\">");

                for (Map.Entry<String, List<String>> gainens : gainenMap.entrySet()) {// 概念マップをまわして
                    String ichibun = gainens.getKey();
                    List<String> infoList = ichibunMap.get(ichibun);// <作品名,作者,URL>
                    String name = infoList.get(0);
                    String author = infoList.get(1);
                    String URL = infoList.get(2);

                    bw.write("<li><a href=\"" + URL + "\" class=\"link\">「" + ichibun + "」</a></li>");// 書き込み
                }
                bw.write("</ul>");

                bw.write("</div>");// 検索結果おわり

                bw.write("</div>");
                // 検索エリアおわり

                bw.write("<br>");
                bw.write("<br>");

                // TOP画面
                // 概念をランダムに選んで、その概念が含まれる一文を表示させている
                bw.write("<div id=\"top\">");
                int i = new java.util.Random().nextInt(gainenData.length);
                String gainen = gainenData[i];

                bw.write("<h1 id =\"gainen\">" + gainen + "</h1><br>");

                // 一文はリスト化
                bw.write("<ul class=\"target-area\" style=\"list-style: none;\">");
                for (Map.Entry<String, List<String>> gainens : gainenMap.entrySet()) {//
                    // 概念マップをまわして
                    String ichibun = gainens.getKey();
                    List<String> gainenList = gainens.getValue();
                    List<String> infoList = ichibunMap.get(ichibun);// <作品名,作者,URL>
                    String name = infoList.get(0);
                    String author = infoList.get(1);
                    String URL = infoList.get(2);
                    if (gainenList.contains(gainen)) {// もしも概念がふくまれていたら

                        bw.write("<li><a href=\"" + URL + "\" class=\"link\">「" + ichibun + "」</a></li>");
                        // bw.write("<li>" + ichibun + "," + name + "," + author + "," + URL +
                        // "</li>");// 書き込み

                    }

                }
                bw.write("</ul>");
                bw.write("</div>");
                bw.write("<br>");

                bw.write("</body>");
                bw.write("</html>");

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            throw e;
        }
    }

}
