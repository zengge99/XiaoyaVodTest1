package com.github.catvod.bean.alist;

import com.github.catvod.spider.Init;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.Toast;

public class LoginDlg {

    public static void showLoginDlg() {
        try {
            Activity activity = Init.getActivity();
            Init.run(() -> {
                if (activity != null && !activity.isFinishing() && !activity.isDestroyed()) {
                    // 创建一个 EditText 用于用户输入
                    final EditText input = new EditText(activity);
                    input.setHint("请输入内容");

                    // 创建 AlertDialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("输入对话框")
                            .setMessage("请填写以下信息：")
                            .setIcon(android.R.drawable.ic_dialog_info) // 设置图标
                            .setView(input) // 添加输入框
                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // 获取用户输入
                                    String userInput = input.getText().toString();
                                    if (!userInput.isEmpty()) {
                                        Toast.makeText(activity, "您输入的内容是：" + userInput, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(activity, "输入不能为空", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(activity, "用户取消了输入", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss(); // 关闭对话框
                                }
                            });

                    // 显示对话框
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }, 500);
        } catch (Exception e) {
        }
    }
}
