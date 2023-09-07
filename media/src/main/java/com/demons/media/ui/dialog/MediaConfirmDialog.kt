package com.demons.media.ui.dialog

import android.view.View
import android.widget.TextView
import com.demons.media.R

/**
 *   @describe 操作提示弹窗页面
 *   @author Yang Qian
 *   @date  2022/12/30 18:13
 */
class MediaConfirmDialog(private val config: Config) : BaseNoticeDialogFragment() {

    private lateinit var tvTitle:TextView
    private lateinit var tvDesc:TextView
    private lateinit var tvCancel:TextView
    private lateinit var tvConfirm:TextView
    private lateinit var vLine:TextView
    override fun layoutResId(): Int {
        return R.layout.dialog_media_confirm
    }

    override fun initViews(view: View) {
        tvTitle = view.findViewById(R.id.tvTitle)
        tvDesc = view.findViewById(R.id.tvDesc)
        tvCancel = view.findViewById(R.id.tvCancel)
        tvConfirm = view.findViewById(R.id.tvConfirm)
        vLine = view.findViewById(R.id.vLine)
        tvCancel.setOnClickListener{
            onOperateListener?.cancel()
            dismiss()
        }
        tvConfirm.setOnClickListener{
            onOperateListener?.confirm()
            dismiss()
        }
        tvTitle.text = config.votingConfirmDialogTitle
        tvDesc.text = config.votingConfirmDialogDes
        tvDesc.visibility = config.votingConfirmDialogDesIsShow

        tvCancel.text = config.votingConfirmDialogLeftButtonDes
        tvConfirm.text = config.votingConfirmDialogRightButtonDes

        vLine.visibility = config.showSingleOperateMode
        tvConfirm.visibility = config.showSingleOperateMode
    }


    private  var onOperateListener: OnOperateListener?=null

    fun setOnOnOperateListener(onOperateListener1: OnOperateListener) {
        this.onOperateListener = onOperateListener1
    }

    interface OnOperateListener {
        fun confirm()
        fun cancel()
    }
    data class Config(
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
