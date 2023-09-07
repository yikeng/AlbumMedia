package com.demons.media.ui.dialog

import android.R
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.annotation.LayoutRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager

/**
 * @author Yangqian
 * @describe 弹窗基类
 * @date 2022/12/2
 */
abstract class BaseNoticeDialogFragment : DialogFragment() {

    private var defaultWidth = WindowManager.LayoutParams.MATCH_PARENT //宽
    private var defaultHeight = WindowManager.LayoutParams.WRAP_CONTENT //高
    private var defaultGravity = Gravity.CENTER //位置
    private var mCancelable = false //默认可取消
    private var mCanceledOnTouchOutside = false //默认点击外部可取消
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(layoutResId(), container, true)
        initViews(view)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val mDialog = super.onCreateDialog(savedInstanceState)
        mDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        mDialog.setCanceledOnTouchOutside(mCanceledOnTouchOutside)
        mDialog.setCancelable(mCancelable)
        val window = mDialog.window
        if (null != window) {
            window.decorView.setPadding(0, 0, 0, 0)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val lp = window.attributes
            lp.width = defaultWidth
            lp.height = defaultHeight
            lp.gravity = defaultGravity
            lp.windowAnimations = R.style.Animation_InputMethod
            window.attributes = lp
        }
        mDialog.setOnKeyListener { _, _, _ -> !mCancelable }
        return mDialog
    }

    /**
     * 设置位置
     *
     * @param gravity
     */
    fun setGravity(gravity: Int) {
        defaultGravity = gravity
    }

    /**
     * 设置宽
     *
     * @param width
     */
    fun setWidth(width: Int) {
        defaultWidth = width
    }

    /**
     * 设置高
     *
     * @param height
     */
    fun setHeight(height: Int) {
        defaultHeight = height
    }

    /**
     * 设置点击返回按钮是否可取消
     *
     * @param cancelable
     */
    override fun setCancelable(cancelable: Boolean) {
        mCancelable = cancelable
    }

    /**
     * 设置点击外部是否可取消
     *
     * @param canceledOnTouchOutside
     */
    fun setCanceledOnTouchOutside(canceledOnTouchOutside: Boolean) {
        mCanceledOnTouchOutside = canceledOnTouchOutside
    }

    /**
     * 设置布局
     *
     * @return
     */
    @LayoutRes
    protected abstract fun layoutResId(): Int

    /**
     * 初始化Views
     *
     * @param view
     */
    protected abstract fun initViews(view: View)

    override fun dismiss() {
        dialog?.apply {
            if (isShowing) {
                super.dismiss()
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            if (!isAdded) {
                val transaction = manager.beginTransaction()
                transaction.add(this, tag)
                transaction.commitAllowingStateLoss()
                transaction.show(this)
            }
        } catch (e: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        dismissAllowingStateLoss()
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissAllowingStateLoss()
    }
}