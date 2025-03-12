package com.github.catvod.bean.alist;

import com.github.catvod.spider.Init;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

public class LoginDlg {

    private static final Object lock = new Object(); // 用于线程同步的锁
    private static boolean isBusy = false; // 是否正在显示对话框
    private static String userInput = ""; // 用户输入

    /**
     * 显示登录对话框，并阻塞后台线程直到对话框关闭
     *
     * @param hint 输入框的提示文本
     * @return 用户输入的内容（如果用户取消输入，返回空字符串）
     */
    public static String showLoginDlg(String hint) {
        synchronized (lock) {
            try {
                Activity activity = Init.getActivity();
                if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                    return ""; // 如果 Activity 无效，直接返回
                }

                // 在主线程显示对话框
                Init.run(() -> {
                    synchronized (lock) {
                        isBusy = true; // 标记为忙碌状态
                    }

                    // 创建一个 EditText 用于用户输入
                    final EditText input = new EditText(activity);
                    input.setHint(hint);

                    // 创建 AlertDialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("输入对话框")
                            .setMessage("请填写以下信息：")
                            .setIcon(android.R.drawable.ic_dialog_info) // 设置图标
                            .setView(input) // 添加输入框
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    synchronized (lock) {
                                        userInput = input.getText().toString();
                                        isBusy = false; // 标记为非忙碌状态
                                        lock.notifyAll(); // 唤醒阻塞的线程
                                    }
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    synchronized (lock) {
                                        Toast.makeText(activity, "用户取消了输入", Toast.LENGTH_SHORT).show();
                                        userInput = ""; // 清空用户输入
                                        isBusy = false; // 标记为非忙碌状态
                                        lock.notifyAll(); // 唤醒阻塞的线程
                                    }
                                }
                            });

                    // 显示对话框
                    AlertDialog dialog = builder.create();
                    dialog.show();
                });

                // 阻塞后台线程，直到对话框关闭
                while (isBusy) {
                    try {
                        lock.wait(); // 等待对话框关闭
                    } catch (InterruptedException e) {
                        break; // 如果线程被中断，退出循环
                    }
                }

                return userInput; // 返回用户输入
            } catch (Exception e) {
                e.printStackTrace(); // 记录异常日志
                return ""; // 发生异常时返回空字符串
            }
        }
    }
}
