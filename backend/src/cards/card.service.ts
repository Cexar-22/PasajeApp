import { AppError } from "../middleware/errorHandler.js";
import type { Card, CardDataSource } from "./card.repository.js";

export class CardService {
  constructor(private readonly repository: CardDataSource) {}

  async getCard(cardId: string): Promise<Card> {
    const card = await this.repository.findById(cardId);
    if (!card) throw new AppError(404, "CARD_NOT_FOUND", "Tarjeta no encontrada.");
    return card;
  }

  async blockCard(cardId: string, confirmed: boolean): Promise<Card> {
    if (!confirmed) {
      throw new AppError(400, "CONFIRMATION_REQUIRED", "Debes confirmar el bloqueo temporal.");
    }
    const card = await this.repository.block(cardId);
    if (!card) throw new AppError(404, "CARD_NOT_FOUND", "Tarjeta no encontrada.");
    return card;
  }

  async updateThreshold(cardId: string, threshold: number): Promise<Card> {
    const card = await this.repository.updateThreshold(cardId, threshold);
    if (!card) throw new AppError(404, "CARD_NOT_FOUND", "Tarjeta no encontrada.");
    return card;
  }
}
