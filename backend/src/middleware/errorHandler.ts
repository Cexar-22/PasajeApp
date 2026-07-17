import type { ErrorRequestHandler, RequestHandler } from "express";
import { ZodError } from "zod";

export class AppError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly details?: Record<string, unknown>,
  ) {
    super(message);
  }
}

export const notFoundHandler: RequestHandler = (_request, response) => {
  response.status(404).json({ code: "ROUTE_NOT_FOUND", message: "Ruta no encontrada." });
};

export const errorHandler: ErrorRequestHandler = (error, _request, response, _next) => {
  if (error instanceof SyntaxError && "status" in error && error.status === 400) {
    response.status(400).json({
      code: "INVALID_JSON",
      message: "El cuerpo JSON no es válido.",
    });
    return;
  }

  if (error instanceof ZodError) {
    response.status(400).json({
      code: "VALIDATION_ERROR",
      message: "Los datos enviados no son válidos.",
      details: error.issues.map((issue) => ({ path: issue.path.join("."), message: issue.message })),
    });
    return;
  }

  if (error instanceof AppError) {
    response.status(error.status).json({
      code: error.code,
      message: error.message,
      ...error.details,
    });
    return;
  }

  console.error("Error interno no controlado", { name: error instanceof Error ? error.name : "Unknown" });
  response.status(500).json({ code: "INTERNAL_ERROR", message: "Ocurrió un error interno." });
};
