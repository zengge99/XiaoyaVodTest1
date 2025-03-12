package com.github.catvod.bean.alist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;

public class LoginDlg {

    public static void showLoginDlg(Context context) {
        // 创建一个 EditText 用于用户输入
        final EditText input = new EditText(context);
        input.setHint("请输入内容");

        // 创建 AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
                            Toast.makeText(context, "您输入的内容是：" + userInput, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(context, "输入不能为空", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context, "用户取消了输入", Toast.LENGTH_SHORT).show();
                        dialog.dismiss(); // 关闭对话框
                    }
                });

        // 显示对话框
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
