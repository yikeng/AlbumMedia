package com.demons.media.ui.dialog

import android.view.View
import com.demons.media.R
import com.demons.media.databinding.DialogMediaConfirmBinding

/**
 *   @describe 操作提示弹窗页面
 *   @author Yang Qian
 *   @date  2022/12/30 18:13
 */
class MediaConfirmDialog(private val config: Config) :
    BaseNoticeDialogFragment<DialogMediaConfirmBinding>() {


    override fun layoutResId(): Int {
        return R.layout.dialog_media_confirm
    }

    override fun initViews() {
        vb?.onClickListener = OnClickListener()
        vb?.config = config
    }

    inner class OnClickListener {
        fun cancel() {
            onOperateListener?.cancel()
            dismiss()
        }

        fun confirm() {
            onOperateListener?.confirm()
            dismiss()
        }
    }

    private  var onOperateListener: OnOperateListener?=null

    fun setOnOnOperateListener(onOperateListener1: OnOperateListener) {
        this.onOperateListener = onOperateListener1
    }

    interface OnOperateListener {
        fun confirm()
        fun cancel()
    }

    class Config(
        //确认弹窗标题
        var votingConfirmDialogTitle: String = "",
        //确认弹窗左边按钮描述
        var votingConfirmDialogLeftButtonDes: String = "",
        //确认弹窗右边按钮描述
        var votingConfirmDialogRightButtonDes: String = "",
        //确认弹窗描述
        var votingConfirmDialogDes: String = "",
        //确认弹窗是否显示描述
        var votingConfirmDialogDesIsShow: Int = View.GONE,
        //是否展示两个操作按钮
        var showSingleOperateMode: Int = View.GONE
    )
}