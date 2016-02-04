package prauto.io;

/**
 * conditional logger removed unless -ea
 */
public interface ErrLog {

	static void errLog(CharSequence s) {
        assert log$(s);
    }
	static boolean log$(CharSequence s) {
        System.err.println(s);
        return true;
    }
}
