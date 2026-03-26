// 1. Add CHEST zone type and treasure box
enum class WorldZoneType {
    ALCHEMIST,
    HERB_SOURCE,
    CHEST  // New type
}

class TreasureChestZone(
    id: String,
    x: Int,
    y: Int,
    radius: Int,
    private val onInteract: (Player) -> Unit
) : WorldZone(id, x, y, radius, WorldZoneType.CHEST) {

    fun interact(player: Player) {
        if (isPlayerInRange(player)) {
            onInteract(player)
        }
    }
}

// In zone initialization:
val treasureChest = TreasureChestZone(
    id = "treasure_chest",
    x = 15,
    y = 20,
    radius = 2
) { player ->
    player.addGold(1)
    player.sendMessage("Ты открыл сундук и получил 1 золото!")
}

// 2. Add distance check in CmdChooseDialogueOption
class CmdChooseDialogueOption : Command() {
    override fun execute(player: Player, args: Array<String>) {
        val alchemistZone = worldZoneManager.getZone("alchemist")

        // Check if player is in alchemist zone
        if (!alchemistZone.isPlayerInRange(player)) {
            player.sendMessage("Ты отошёл слишком далеко от Алхимика")
            return
        }

        // Continue with dialogue option handling
        val optionIndex = args[0].toInt()
        player.activeDialogue?.chooseOption(optionIndex)
    }
}

// 3. Extend NpcMemory and add zone tracking
data class NpcMemory(
    val npcId: String,
    val playerId: String,
    var sawPlayerNearSource: Boolean = false,  // New field
    var lastInteractionTime: Long = 0,
    var dialogueState: String = "start"
) {
    fun updateSourceVisit() {
        sawPlayerNearSource = true
    }

    fun hasVisitedSource(): Boolean = sawPlayerNearSource
}

// In Alchemist NPC dialogue logic:
class AlchemistNPC : NPC() {

    fun getDialogueBasedOnMemory(player: Player): String {
        val memory = npcMemoryManager.getMemory(this.id, player.id)

        return when {
            memory.hasVisitedSource() -> {
                "Вижу, ты хотя бы дошёл до места, где растёт трава, ты её принес?"
            }
            else -> {
                "Привет, путник! Мне нужна трава из источника на востоке."
            }
        }
    }

    override fun onZoneEnter(player: Player, zone: WorldZone) {
        if (zone.type == WorldZoneType.HERB_SOURCE) {
            val memory = npcMemoryManager.getMemory(this.id, player.id)
            memory.updateSourceVisit()
            npcMemoryManager.saveMemory(memory)
        }
    }
}

// Zone manager to track player movement
class WorldZoneManager {
    private val zones = mutableMapOf<String, WorldZone>()

    fun updatePlayerPosition(player: Player, x: Int, y: Int) {
        player.lastX = x
        player.lastY = y

        // Check zone entries/exits
        zones.values.forEach { zone ->
            val wasInZone = player.activeZones.contains(zone.id)
            val isInZone = zone.isInRange(x, y)

            when {
                !wasInZone && isInZone -> {
                    player.activeZones.add(zone.id)
                    zone.onEnter(player)
                }
                wasInZone && !isInZone -> {
                    player.activeZones.remove(zone.id)
                    zone.onExit(player)
                }
            }
        }
    }
}