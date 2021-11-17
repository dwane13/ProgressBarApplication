package com.example.progressbarapplication

import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.annotation.FloatRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.properties.Delegates

/**
 * Как я говорил рекрутеру - мне приходилось делать подобный элемент для работы (где, он, кстати, не пригодился),
 * так что я взял за основу то, что у меня уже было.
 *
 * В текущей реализации не применяется паддинг, ибо мне показалось это избыточным для тестового задания.
 * Также я не совсем понял часть ТЗ про "Размеры прогресса должны оттакливаться от размера View",
 * но надеюсь, что все сделал как и нужно было, по крайней мере match_parent и условные 100dp работают.
 * Разве что wrap_content делать не стал.
 *
 * Также не стал делать метод для изменения длительности анимации и ее отключения. Просто смените [mDuration]
 * У меня еще были некоторые проблемы с определением размеров, так что прикинул на глаз.
 */
class CircularProgressIndicator @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null
) : View(context, attributeSet) {

    // region private fields



    private var mDuration = 1000L

    // радиус нашего прогресс-бара
    private var mRadius = 0.0f

    // квадрат, в рамках которого будет нарисован прогресс бар
    private var mDrawingRect = RectF()

    // цвет для отображения пустой части прогресс бара
    private var mStrokeColor by Delegates.notNull<Int>()

    // ширина "мазка"
    private var mStrokeWidth by Delegates.notNull<Int>()

    // цвет заполнения прогресс бара
    private var mFillColor by Delegates.notNull<Int>()

    // градиентный цвет заполнения прогресс бара
    private var mGradientColor by Delegates.notNull<Int>()

    // центр view
    private lateinit var mCenter: PointF

    // не думаю, что утечка возможна, но лучше перестраховаться
    private var mAnimationJob: Job? = null

    // целевой процент прогресса
    private var mTargetProgress = 0.0f

    // текущий процент прогресса
    private var mFilledPercentage = 0.0f
        set(value) {
            field = value
            invalidate()
        }

    // Аниматор, для игнорирования выключенных системных анимаций
    private var mAnimator = ProgressAnimator().apply { duration = mDuration }


    init {
        // получаем атрибуты
        val attr = context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.CircularProgressIndicator,
            0,
            0
        )
        // устанавливаем значения
        getStrokeColorIfAny(attr)
        getFillColorIfAny(attr)
        getGradientColorIfAny(attr)
        getStrokeWidth(attr)
        getFilledPercent(attr)
        attr.recycle()
    }

    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            strokeWidth = mStrokeWidth.toFloat()
        }

    // вроде в вашем макете есть градиент
    private lateinit var mGradient: SweepGradient



    // endregion
    // region private methods



    private fun getStrokeWidth(attr: TypedArray) {
        mStrokeWidth = attr.getDimensionPixelSize(
            R.styleable.CircularProgressIndicator_strokeWidth, 20
        )
    }

    private fun getFillColorIfAny(attr: TypedArray) {
        mFillColor = attr.getColor(
            R.styleable.CircularProgressIndicator_fillColor,
            context.getColor(android.R.color.holo_blue_dark)
        )
    }

    private fun getGradientColorIfAny(attr: TypedArray) {
        mGradientColor = attr.getColor(
            R.styleable.CircularProgressIndicator_gradientColor,
            context.getColor(android.R.color.holo_purple)
        )
    }

    private fun getStrokeColorIfAny(attr: TypedArray) {
        mStrokeColor = attr.getColor(
            R.styleable.CircularProgressIndicator_strokeColor,
            context.getColor(android.R.color.holo_blue_light)
        )
    }

    private fun getFilledPercent(attr: TypedArray) {
        val percent = attr.getFloat(
            R.styleable.CircularProgressIndicator_progress,
            0f
        )
        mFilledPercentage = when {
            percent > 100 -> 100f
            percent < 0 -> 0f
            else -> percent
        }
    }

    /**
     * Метод создает экземпляр [RectF]. Каждая сторона удаленна от центра на дистанцию [mRadius]
     */
    private fun getDrawingRectangle(): RectF {
        return RectF(
            mCenter.x - mRadius,
            mCenter.y - mRadius,
            mCenter.x + mRadius,
            mCenter.y + mRadius
        )
    }

    /**
     * Получаем центр нашей view
     */
    private fun calcViewCenter() = PointF(width / 2f, height / 2f)


    /**
     * Просчитываем радиус исходя из меньшей из сторон
     */
    private fun calcRadius(): Float {
        return (min(width, height) / 2f - (mStrokeWidth / 2))
    }

    private fun setFilledPercent(@FloatRange(from = 0.0, to = 1.0) progress: Float) {
        mFilledPercentage = progress
    }

    /**
     * Изменяем значение прогресса при помощи [ObjectAnimator], который, по всей видимости,
     * использует системный scale
     */
    private fun changeProgressWithSystemAnimSettings(
        oldProgress: Float,
        newProgress: Float
    ) {
        mAnimationJob = CoroutineScope(Dispatchers.Main).launch {
            ObjectAnimator.ofFloat(
                this@CircularProgressIndicator,
                "mFilledPercentage",
                oldProgress,
                newProgress
            ).apply {
                duration = mDuration
                start()
            }
        }
    }

    /**
     * Плюем на системный скейл и все равно анимируем при помощи нашего [ProgressAnimator]
     */
    private fun changeProgressWithForcedAnimation(
        oldProgress: Float,
        newProgress: Float
    ) {
        mAnimator.setProgressRange(oldProgress, newProgress)
        startAnimation(mAnimator)
    }


    // endregion
    // region public methods


    fun setProgress(@FloatRange(from = 0.0, to = 1.0) progress: Float, forceAnimation: Boolean = false) {
        mTargetProgress = progress * 100

        if (forceAnimation) {
            changeProgressWithForcedAnimation(mFilledPercentage, mTargetProgress)
        } else {
            changeProgressWithSystemAnimSettings(mFilledPercentage, mTargetProgress)
        }
    }


    // endregion
    // region overrides


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        mRadius = calcRadius()
        mCenter = calcViewCenter()
        mDrawingRect = getDrawingRectangle()
        mGradient = getGradient()
    }

    private fun getGradient(): SweepGradient {
        val gradient = SweepGradient(mCenter.x,  mCenter.y, mFillColor, mGradientColor).apply {
            val matrix = Matrix()
            matrix.postRotate(100f, mDrawingRect.width() / 2, mDrawingRect.height() / 2)
            setLocalMatrix(matrix)
        }
        return gradient
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // рисуем основу нашего прогресс бара
        paint.shader = null
        paint.color = mStrokeColor
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        // рисуем с градуса 135 3/4 круга
        canvas.drawArc(mDrawingRect, 155f, 230f, false, paint)

        // меняем стиль для отрисовки текста
        paint.color = mFillColor
        paint.style = Paint.Style.FILL
        paint.color = mFillColor

        // устанавливаем размер текста в 66% от радиуса
        paint.textSize = mRadius / 2f
        canvas.drawText("${mFilledPercentage.toInt()}%", mCenter.x, mCenter.y, paint)

        // рисуем арку прогресса сверху
        paint.style = Paint.Style.STROKE
        paint.shader = mGradient
        canvas.drawArc(
            mDrawingRect,
            155f,
            230f / 100f * mFilledPercentage,
            false,
            paint
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mAnimationJob?.cancel()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable(PARENT_STATE))
            mFilledPercentage = state.getFloat(PROGRESS_STATE)
            mTargetProgress = state.getFloat(PROGRESS_STATE)
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        return Bundle().apply {
            putParcelable(PARENT_STATE, super.onSaveInstanceState())
            putFloat(PROGRESS_STATE, mTargetProgress)
        }
    }


    // endregion
    // region classes


    private inner class ProgressAnimator : Animation() {

        private var mFrom: Float = 0f
        private var mTo: Float = 0f

        override fun applyTransformation(
            interpolatedTime: Float,
            transformation: Transformation?
        ) {
            super.applyTransformation(interpolatedTime, transformation)

            val angle = when {
                mFrom < mTo -> {
                    mFrom + ((mTo - mFrom) * interpolatedTime)
                }
                mFrom > mTo -> {
                    mFrom - ((mFrom - mTo) * (interpolatedTime))
                }
                else -> {
                    return
                }
            }

            this@CircularProgressIndicator.setFilledPercent(angle)
        }

        fun setProgressRange(
            from: Float,
            to: Float
        ) {
            mFrom = from
            mTo = to
        }
    }



    //endregion


    companion object {
        private const val PARENT_STATE = "par_state"
        private const val PROGRESS_STATE = "prog_state"
    }
}
