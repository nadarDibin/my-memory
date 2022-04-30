package com.example.mymemory.models

import com.example.mymemory.utils.DEFAULT_ICONS

class MemoryGame(private val gameGameBoard: GameBoard, customImages: List<String>?) {

    var numPairsFound = 0
    private var numMoves = 0
    private var unmatchedFaceUpCardPosition: Int? = null // TODO: Better Name

    val cards: List<MemoryCard> = if (customImages == null) {
        val chosenImages = DEFAULT_ICONS.shuffled().take(gameGameBoard.getNumPairs())
        val randomizedImages = (chosenImages + chosenImages).shuffled()
        randomizedImages.map { MemoryCard(it) }
    } else {
        val randomizedImages = (customImages + customImages).shuffled()
        randomizedImages.map { MemoryCard(it.hashCode(), it) }
    }

    fun makeMove(position: Int): Boolean {
        numMoves++
        val card = cards[position]
        var isPair = false

        if (unmatchedFaceUpCardPosition == null) {
            resetCards()
            unmatchedFaceUpCardPosition = position
        } else {
            isPair = checkIfPair(unmatchedFaceUpCardPosition!!, position)
            if (isPair) updatePairStatus(unmatchedFaceUpCardPosition!!, position)
            unmatchedFaceUpCardPosition = null
        }

        flipCard(card)
        return isPair
    }

    private fun checkIfPair(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) return false

        return true
    }

    private fun updatePairStatus(unmatchedFaceUpCardPosition: Int, cardPosition: Int) {
        cards[unmatchedFaceUpCardPosition].isMatched = true
        cards[cardPosition].isMatched = true
        numPairsFound++
    }

    private fun flipCard(card: MemoryCard) {
        card.isFaceUp = !card.isFaceUp
    }

    private fun resetCards() {
        cards.forEach { card -> if (!card.isMatched) card.isFaceUp = false }
    }

    fun hasWonGame(): Boolean {
        return numPairsFound == gameGameBoard.getNumPairs()
    }

    fun isCardFaceUp(position: Int): Boolean {
        return cards[position].isFaceUp
    }

    fun getGameMoves(): Int {
        return numMoves / 2
    }
}
