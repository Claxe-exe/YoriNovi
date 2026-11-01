import com.fallowthis.app.PhoneCore;
import org.json.*;
import java.nio.file.*;
import java.util.*;
public class NativeCoreTest {
    public static void main(String[] args)throws Exception{
        JSONObject catalog=new JSONObject(Files.readString(Path.of("android/app/src/main/assets/fallow-catalog.json")));
        JSONArray tests=new JSONArray(Files.readString(Path.of("artifacts/native-test/golden.json")));int checks=0;
        for(int i=0;i<tests.length();i++){JSONObject test=tests.getJSONObject(i);JSONArray actual=PhoneCore.classify(test.getJSONObject("article"),catalog),expected=test.getJSONArray("expected");require(actual.length()==expected.length(),"Type count: "+test);for(int j=0;j<expected.length();j++)for(String field:new String[]{"identity","title","summary","type","release_date","release_precision"}){String nativeKey=field,jsKey=field.equals("release_date")?"releaseDate":field.equals("release_precision")?"releasePrecision":field;require(String.valueOf(actual.getJSONObject(j).opt(nativeKey)).equals(String.valueOf(expected.getJSONObject(j).opt(jsKey))),field+" mismatch: "+actual+" / "+expected);checks++;}}
        JSONObject settings=catalog.getJSONObject("defaults");require(PhoneCore.quiet(settings,PhoneCore.epoch("2026-11-01T21:00:00Z")),"Overnight quiet hours");require(!PhoneCore.quiet(settings,PhoneCore.epoch("2026-11-01T09:00:00Z")),"Daytime quiet hours");
        JSONObject rss=catalog.getJSONArray("sources").getJSONObject(4);JSONArray parsed=PhoneCore.parseSource(rss,"<rss><channel><item><title>Lycoris Recoil season 2 confirmed!</title><link>https://www.toei-animation.com/example/</link><pubDate>Sun, 01 Nov 2026 00:00:00 +0000</pubDate><description>Official announcement</description></item></channel></rss>");require(parsed.length()==1,"RSS parser");require(PhoneCore.classify(parsed.getJSONObject(0),catalog).length()==1,"RSS → classifier chain");
        require(PhoneCore.published("2026.02.31")==null,"Invalid source date");
        if(Files.exists(Path.of("artifacts/native-test/live.json"))){JSONArray fixtures=new JSONArray(Files.readString(Path.of("artifacts/native-test/live.json")));for(int i=0;i<fixtures.length();i++){JSONObject f=fixtures.getJSONObject(i);JSONArray articles=PhoneCore.parseSource(f.getJSONObject("source"),f.getString("text"));require(articles.length()==f.getInt("count"),"Live source article count: "+f.getJSONObject("source").getString("id"));Set<String> actual=new TreeSet<>(),expected=new TreeSet<>();for(int j=0;j<articles.length();j++){JSONArray out=PhoneCore.classify(articles.getJSONObject(j),catalog);for(int k=0;k<out.length();k++)actual.add(out.getJSONObject(k).getString("identity"));}JSONArray ids=f.getJSONArray("identities");for(int j=0;j<ids.length();j++)expected.add(ids.getString(j));require(actual.equals(expected),"Live source classification: "+f.getJSONObject("source").getString("id")+" "+actual+" / "+expected);}System.out.println("PASS: "+fixtures.length()+" real official source HTML/RSS fixtures match JavaScript parsing and classification.");}
        System.out.println("PASS: "+tests.length()+" native/JavaScript parity cases, "+checks+" event-field assertions, quiet hours, RSS chain, invalid date.");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
