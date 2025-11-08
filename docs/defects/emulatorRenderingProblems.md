# Emulator Rendering Problems

## No tiles rendered 

It appears the problem could be in:

private fun generateGrid in TiledVertexGrid.kt

the tiles are set in fun addTileMapLayer in LevelBuilder.kt

Check out the title tileset closely - in opening scene - messed up in emulator

THEME_LIGHTING -> {
tileMapIndex = R.drawable.titletileset
priority = SortConstants.OVERLAY //hack!
}

