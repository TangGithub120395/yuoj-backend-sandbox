import java.security.Permission;

/**
 * @author tangzhen
 * @version 1.0
 * @date 2024/3/3 14:33
 */
public class DefaultSecurityManager extends SecurityManager {
    @Override
    public void checkPermission(Permission perm) {

    }

    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("exec权限不足:" + cmd);
    }


    @Override
    public void checkRead(String file) {
//        throw new SecurityException("read权限不足:" + file);
    }

    @Override
    public void checkWrite(String file) {
        throw new SecurityException("write权限不足:" + file);
    }

    @Override
    public void checkDelete(String file) {
        throw new SecurityException("delete权限不足:" + file);
    }

    @Override
    public void checkConnect(String host, int port) {
        throw new SecurityException("connect权限不足:" + host + ":" + port);
    }
}
