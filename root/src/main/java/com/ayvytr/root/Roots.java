package com.ayvytr.root;

import android.text.TextUtils;

import com.ayvytr.root.container.Command;
import com.ayvytr.root.container.Result;
import com.ayvytr.root.container.Shell;
import com.ayvytr.root.exception.PermissionException;
import com.ayvytr.root.utils.Remounter;
import com.ayvytr.root.utils.RootUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Root管理类，有功能：判断系统有没有被Root，获取Root权限，给文件添加/取消读写执行权限等.
 *
 * @author Ayvytr <a href="https://github.com/Ayvytr" target="_blank">'s GitHub</a>
 * @since 1.0.0
 */

public enum Roots
{
    THIS;

    private static boolean accessRoot = false;
    private boolean hasRooted;
    private boolean hasGivenPermission = false;
    private long lastPermissionCheck = -1;

    public static Roots get()
    {
        return THIS;
    }

    /**
     * Check if the device is rooted
     * <p>
     * This function check if the system has SU file. Note that though SU file exits, it might not
     * work.
     * </p>
     *
     * @return this device is rooted or not.
     */
    public boolean hasRooted()
    {
        for(String path : Constants.SU_BINARY_DIRS)
        {
            File su = new File(path, Constants.SU);
            if(su.exists())
            {
                hasRooted = true;
                break;
            }
        }

        return hasRooted;
    }

    /**
     * Try to obtain the root access.
     * <p>
     * This function might lead to a popup to users, and wait for the input : grant or decline.
     * </p>
     *
     * @return the app has been granted the root permission or not.
     */
    public boolean requestRootPermission()
    {
        if(!hasGivenPermission)
        {
            hasGivenPermission = accessRoot();
            lastPermissionCheck = System.currentTimeMillis();
        }
        else
        {
            if(lastPermissionCheck < 0
                    || System.currentTimeMillis() - lastPermissionCheck
                    > Constants.PERMISSION_EXPIRE_TIME)
            {
                hasGivenPermission = accessRoot();
                lastPermissionCheck = System.currentTimeMillis();
            }
        }

        return hasGivenPermission;
    }


    /**
     * Copy a file into destination dir.
     *
     * @param source         the source file path.
     * @param destinationDir the destination dir path.
     * @return the operation result.
     */
    public boolean copyFile(String source, String destinationDir)
    {
        if(TextUtils.isEmpty(destinationDir) || TextUtils.isEmpty(source))
        {
            return false;
        }

        File sourceFile = new File(source);
        File desFile = new File(destinationDir);
        if(!sourceFile.exists() || !desFile.isDirectory())
        {
            return false;
        }

        if(remount(destinationDir, "rw"))
        {
            return runCommand("cat '" + source + "' > " + destinationDir).getResult();
        }
        else
        {
            return false;
        }
    }

    /**
     * Remount file system.
     *
     * @param path      the path you want to remount
     * @param mountType the mount type, including, <i>"ro", read only, "rw" , read and write</i>
     * @return the operation result.
     */
    public boolean remount(String path, String mountType)
    {
        if(TextUtils.isEmpty(path) || TextUtils.isEmpty(mountType))
        {
            return false;
        }

        if(mountType.equalsIgnoreCase("rw") || mountType.equalsIgnoreCase("ro"))
        {
            return Remounter.remount(path, mountType);
        }
        else
        {
            return false;
        }

    }

    /**
     * Run raw commands in default shell.
     *
     * @param command the command string.
     * @return the result {@link Result} of running the command.
     */
    public Result runCommand(String command)
    {
        final Result.ResultBuilder builder = Result.newBuilder();
        if(TextUtils.isEmpty(command))
        {
            return builder.setFailed().build();
        }

        final StringBuilder infoSb = new StringBuilder();
        Command commandImpl = new Command(command)
        {

            @Override
            public void onUpdate(int id, String message)
            {
                infoSb.append(message + "\n");
            }

            @Override
            public void onFinished(int id)
            {
                builder.setCustomMessage(infoSb.toString());
            }

        };

        try
        {
            Shell.startRootShell().add(commandImpl).waitForFinish();
        } catch(InterruptedException e)
        {
            e.printStackTrace();
            builder.setCommandFailedInterrupted();
        } catch(IOException e)
        {
            e.printStackTrace();
            builder.setCommandFailed();
        } catch(TimeoutException e)
        {
            e.printStackTrace();
            builder.setCommandFailedTimeout();
        } catch(PermissionException e)
        {
            e.printStackTrace();
            builder.setCommandFailedDenied();
        }

        return builder.build();
    }

    /**
     * Get screen shot.
     *
     * @param path the path with file name and extend name.
     * @return the operation result.
     */
    public boolean screenCap(String path)
    {

        if(TextUtils.isEmpty(path))
        {
            return false;
        }
        Result res = runCommand(Constants.COMMAND_SCREENCAP + path);

        return res.getResult();
    }

    /**
     * Record screen for 30s. This function is ONLY supported on Android 4.4 and upper.
     *
     * @param path the path with file name and extend name.
     * @return the operation result.
     */
    public boolean screenRecord(String path)
    {
        return screenRecord(path, Constants.SCREENRECORD_BITRATE_DEFAULT,
                Constants.SCREENRECORD_TIMELIMIT_DEFAULT);
    }

    /**
     * Record screen. This function is ONLY supported on Android 4.4 and upper.
     *
     * @param path    the path with file name and extend name.
     * @param bitRate the bate rate in bps, i.e., 4000000 for 4M bps.
     * @param time    the recording time in seconds
     * @return the operation result.
     */

    public boolean screenRecord(String path, long bitRate, long time)
    {

        if(!RootUtils.isKitKatUpper() || TextUtils.isEmpty(path))
        {
            return false;
        }
        Result res = runCommand(Constants.COMMAND_SCREENRECORD + "--bit-rate " + bitRate
                + " --time-limit " + time + " " + path);
        return res.getResult();
    }

    /**
     * Check if a process is running.
     *
     * @param processName the name of process. For user app, the process name is
     *                    its package name.
     * @return whether this process is running.
     */
    public boolean isProcessRunning(String processName)
    {

        if(TextUtils.isEmpty(processName))
        {
            return false;
        }
        Result infos = runCommand(Constants.COMMAND_PS);
        return infos.getMessage().contains(processName);
    }

    /**
     * Kill a process by its name.
     *
     * @param processName the name of this process. For user apps, the process
     *                    name is its package name.
     * @return the operation result.
     */
    public boolean killProcess(String processName)
    {
        if(TextUtils.isEmpty(processName))
        {
            return false;
        }
        Result res = runCommand(Constants.COMMAND_PIDOF + processName);

        if(!TextUtils.isEmpty(res.getMessage()))
        {
            return killProcess(res.getMessage());
        }
        else
        {
            return false;
        }
    }

    /**
     * Kill a process by its process id.
     *
     * @param processId PID of the target process.
     * @return the operation result.
     */
    public boolean killProcess(int processId)
    {
        Result res = runCommand(Constants.COMMAND_KILL + processId);
        return res.getResult();
    }

    /**
     * Restart the device.
     */
    public void restartDevice()
    {
        killProcess("zygote");
    }

    private boolean accessRoot()
    {
        boolean result;
        accessRoot = false;

        Command commandImpl = new Command("id")
        {
            @Override
            public void onUpdate(int id, String message)
            {
                if(message != null && message.toLowerCase().contains("uid=0"))
                {
                    accessRoot = true;
                }
            }

            @Override
            public void onFinished(int id)
            {

            }
        };

        try
        {
            Shell.startRootShell().add(commandImpl).waitForFinish();
            result = accessRoot;
        } catch(Exception e)
        {
            e.printStackTrace();
            result = false;
        }

        return result;

    }

    public void requestReadPermisson(String path)
    {
        runCommand("chmod o+r " + path);
    }

    public void cancelReadPermission(String path)
    {
        runCommand("chmod o-r " + path);
    }

    public void requestWritePermission(String path)
    {
        runCommand("chmod o+rw " + path);
    }

    public void cancelWritePermission(String path)
    {
        runCommand("chmod o-rw " + path);
    }

    public void requestExecutePermission(String path)
    {
        runCommand("chmod o+x " + path);
    }

    public void cancelExecutePermission(String path)
    {
        runCommand("chmod o-x " + path);
    }

    public void requestReadDbWithJournalPermission(String path)
    {
        //读取数据库需要把数据表和-journal数据表一起加上读权限
        runCommand("chmod o+r " + path);
        runCommand("chmod o+r " + path + "-journal");
    }

    public void cancelReadDbWithJournalPermission(String path)
    {
        runCommand("chmod o-r " + path);
        runCommand("chmod o-r " + path + "-journal");
    }

    public void requestWriteDbWithJournalPermission(String path)
    {
        //读取数据库需要把数据表和-journal数据表一起加上读权限
        runCommand("chmod o+rw " + path);
        runCommand("chmod o+rw " + path + "-journal");
    }

    public void cancelWriteDbWithJournalPermission(String path)
    {
        runCommand("chmod o-rw " + path);
        runCommand("chmod o-rw " + path + "-journal");
    }

    public void requestFullPermission(String path)
    {
        runCommand("chmod o+rwx " + path);
    }

    public void cancelFullPermission(String path)
    {
        runCommand("chmod o-rwx " + path);
    }
}
