package o.j.tw;

public class M {
    public static Object fieldX;
    public static void run() {
    }

    public static void main(String[] args) throws Throwable {
        var met = M.class.getMethod("main", String[].class);
        System.out.println(met);
        M.class.getMethod("run").invoke(null);
        var field = M.class.getDeclaredField("fieldX");

        System.out.println(field.get(null));
        field.set(null, new Object());
        System.out.println(field.get(null));
    }
}
