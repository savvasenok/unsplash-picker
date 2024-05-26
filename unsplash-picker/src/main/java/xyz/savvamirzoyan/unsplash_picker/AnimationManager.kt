package xyz.savvamirzoyan.unsplash_picker

import android.animation.ValueAnimator
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart

private const val DURATION = 300L

class AnimationManager {

    fun animateHeight(
        from: Float,
        to: Float,
        onStart: (() -> Unit)? = null,
        onEnd: (() -> Unit)? = null,
        onChange: ((height: Int) -> Unit)? = null
    ) {
        ValueAnimator.ofFloat(from, to).apply {

            duration = DURATION

            if (onStart != null) doOnStart { onStart() }

            if (onEnd != null) doOnEnd { onEnd() }

            if (onChange != null) addUpdateListener { onChange((it.animatedValue as Float).toInt()) }

            start()
        }
    }
}