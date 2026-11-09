package com.example.core.traps

import com.example.core.model.Direction
import com.example.core.model.Position

data class MovingHazard(
    var currentPos: Position,
    var direction: Direction,
    val minPos: Position,
    val maxPos: Position
)

data class CountdownTile(
    val pos: Position,
    val maxTurns: Int,
    var remainingTurns: Int
)

data class DisappearingTile(
    val pos: Position,
    var isStepped: Boolean = false,
    var isCrumbled: Boolean = false
)

data class SwitchBridgeLink(
    val switchPos: Position,
    var isActivated: Boolean = false,
    val bridgePositions: List<Position>
)

data class ShiftingWallGroup(
    val positions: List<Position>,
    val cadenceTurns: Int,
    var isClosed: Boolean = true
)

data class FakeSafeTile(
    val pos: Position,
    var isTriggered: Boolean = false
)

data class TrapConfiguration(
    val movingHazards: MutableList<MovingHazard> = mutableListOf(),
    val countdownTiles: MutableList<CountdownTile> = mutableListOf(),
    val disappearingTiles: MutableList<DisappearingTile> = mutableListOf(),
    val switchBridgeLinks: MutableList<SwitchBridgeLink> = mutableListOf(),
    val shiftingWalls: MutableList<ShiftingWallGroup> = mutableListOf(),
    val fakeSafeTiles: MutableList<FakeSafeTile> = mutableListOf(),
    val portalsA: MutableList<Position> = mutableListOf(),
    val portalsB: MutableList<Position> = mutableListOf(),
    val fakeGoals: MutableList<Position> = mutableListOf(),
    val reverseControlTiles: MutableList<Position> = mutableListOf(),
    val invisiblePaths: MutableList<Position> = mutableListOf(),
    val memoryTiles: MutableList<Position> = mutableListOf()
) {
    fun deepCopy(): TrapConfiguration {
        return TrapConfiguration(
            movingHazards = movingHazards.map { it.copy() }.toMutableList(),
            countdownTiles = countdownTiles.map { it.copy() }.toMutableList(),
            disappearingTiles = disappearingTiles.map { it.copy() }.toMutableList(),
            switchBridgeLinks = switchBridgeLinks.map { it.copy(bridgePositions = it.bridgePositions.toList()) }.toMutableList(),
            shiftingWalls = shiftingWalls.map { it.copy(positions = it.positions.toList()) }.toMutableList(),
            fakeSafeTiles = fakeSafeTiles.map { it.copy() }.toMutableList(),
            portalsA = portalsA.toMutableList(),
            portalsB = portalsB.toMutableList(),
            fakeGoals = fakeGoals.toMutableList(),
            reverseControlTiles = reverseControlTiles.toMutableList(),
            invisiblePaths = invisiblePaths.toMutableList(),
            memoryTiles = memoryTiles.toMutableList()
        )
    }
}
