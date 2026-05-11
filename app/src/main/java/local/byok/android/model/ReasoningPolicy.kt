package local.byok.android.model

object ReasoningPolicy {
    fun resolve(prompt: String, settings: ModelSettings): ReasoningEffort {
        if (settings.reasoning != ReasoningChoice.Auto) {
            return when (settings.reasoning) {
                ReasoningChoice.None -> ReasoningEffort.None
                ReasoningChoice.Low -> ReasoningEffort.Low
                ReasoningChoice.Medium -> ReasoningEffort.Medium
                ReasoningChoice.High -> ReasoningEffort.High
                ReasoningChoice.XHigh -> ReasoningEffort.XHigh
                ReasoningChoice.Auto -> ReasoningEffort.Medium
            }
        }

        val text = prompt.lowercase()
        val compact = listOf("翻译", "润色", "格式", "摘要", "translate", "polish", "format", "summarize")
        val complex = listOf("代码", "bug", "方案", "设计", "分析", "架构", "重构", "数学", "多文件", "code", "debug", "architecture", "refactor")

        var effort = when {
            compact.any(text::contains) -> if (prompt.length < 600) ReasoningEffort.None else ReasoningEffort.Low
            complex.any(text::contains) -> ReasoningEffort.High
            prompt.length < 180 -> ReasoningEffort.Low
            prompt.length > 2500 -> ReasoningEffort.High
            else -> ReasoningEffort.Medium
        }
        if (listOf("多文件", "架构级", "复杂数学").any(text::contains)) effort = ReasoningEffort.XHigh
        return effort.coerceAtMost(
            when (settings.strategy) {
                QualityStrategy.Speed -> ReasoningEffort.Medium
                QualityStrategy.Balanced -> ReasoningEffort.High
                QualityStrategy.Saving -> ReasoningEffort.Low
                QualityStrategy.Quality -> ReasoningEffort.XHigh
            }
        )
    }

    private fun ReasoningEffort.coerceAtMost(max: ReasoningEffort): ReasoningEffort {
        val order = listOf(
            ReasoningEffort.None,
            ReasoningEffort.Low,
            ReasoningEffort.Medium,
            ReasoningEffort.High,
            ReasoningEffort.XHigh
        )
        return order[minOf(order.indexOf(this), order.indexOf(max))]
    }
}

object TokenEstimator {
    fun estimate(text: String): Int {
        if (text.isBlank()) return 0
        val cjk = text.count { it.code in 0x3400..0x9fff }
        val rest = text.length - cjk
        return maxOf(1, kotlin.math.ceil(cjk * 0.8 + rest / 4.0).toInt())
    }
}
