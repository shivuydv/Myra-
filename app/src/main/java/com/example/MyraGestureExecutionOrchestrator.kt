package com.example

import android.content.Context

class MyraGestureExecutionOrchestrator(private val context: Context) {
    private val geometry = MyraScreenGeometryEngine(context)
    private val boundary = MyraSafeGestureBoundary(context)
    private val coordinate = MyraCoordinateActionEngine()
    private val directionEngine = MyraGestureIntelligence()
    private val contextEngine = MyraGestureContextEngine()
    private val strategySelector = MyraGestureStrategySelector()
    private val intentResolver = MyraGestureIntentResolver()
    private val goalPlanner = MyraGestureGoalPlanner()
    private val verifier = MyraGestureResultVerifier()
    private val confidenceEngine = MyraGestureConfidenceEngine()
    private val learning = MyraGestureLearning(context)

    suspend fun execute(
        command: String, section: MyraScreenSection = MyraScreenSection.UNKNOWN
    ): MyraActionResult {
        if (command.isBlank()) return MyraActionResult(
            MyraActionStatus.FAILED,"Gesture command खाली है."
        )
        val direction = directionEngine.parseDirection(command)
            ?: return MyraActionResult(MyraActionStatus.FAILED,"Direction समझ नहीं आई.")
        val g = geometry.getGeometry()
        val strategy = strategySelector.select(command,direction,g.height.toFloat(),g.width.toFloat())
        val pattern = MyraGesturePatternRecognition().recognize(
            direction,strategy.distanceRatio,strategy.duration,section
        )
        val intent = intentResolver.resolve(command,pattern.pattern,section)
        if (intent.confidence < .60f) return MyraActionResult(
            MyraActionStatus.WAITING,"Gesture intent clear नहीं है."
        )
        val goal = goalPlanner.createGoal(intent.intent,direction,strategy)
        val oldPackage = MyraScreenState.currentPackage
        val oldClass = MyraScreenState.currentClass
        val c = contextEngine.createContext(
            goal.direction,g.width.toFloat(),g.height.toFloat(),command
        )
        val distance = if (goal.direction == MyraGestureDirection.LEFT ||
            goal.direction == MyraGestureDirection.RIGHT) g.width*goal.distanceRatio
            else g.height*goal.distanceRatio
        val end = when (goal.direction) {
            MyraGestureDirection.UP -> c.startX to c.startY-distance
            MyraGestureDirection.DOWN -> c.startX to c.startY+distance
            MyraGestureDirection.LEFT -> c.startX-distance to c.startY
            MyraGestureDirection.RIGHT -> c.startX+distance to c.startY
        }
        val s = boundary.clamp(c.startX,c.startY)
        val e = boundary.clamp(end.first,end.second)
        val result = coordinate.swipe(s.first,s.second,e.first,e.second,goal.duration)
        if (!result.success) {
            learning.record(MyraGestureExperience(oldPackage,direction,goal.distanceRatio,goal.duration,false))
            return result
        }
        val v = verifier.verify(oldPackage,oldClass)
        val confidence = confidenceEngine.calculate(
            packageChanged = MyraScreenState.currentPackage != oldPackage,
            classChanged = MyraScreenState.currentClass != oldClass,
            visualChanged = v.changed,
            visualScore = if (v.changed) .30f else 0f,
            ocrChanged = v.changed
        )
        learning.record(MyraGestureExperience(
            oldPackage,direction,goal.distanceRatio,goal.duration,confidence.reliable
        ))
        return if (confidence.reliable) MyraActionResult(
            MyraActionStatus.SUCCESS,
            "Gesture complete. Confidence: ${(confidence.score*100).toInt()}%"
        ) else MyraActionResult(
            MyraActionStatus.RETRY,
            "Gesture भेजा गया, लेकिन result reliably verify नहीं हुआ."
        )
    }
}
