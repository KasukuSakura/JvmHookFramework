package o.j.tw;

public class M {
    public static void main(String[] args) throws Throwable {
        var met = M.class.getMethod("main", String[].class);
        System.out.println(met);
    }
}
