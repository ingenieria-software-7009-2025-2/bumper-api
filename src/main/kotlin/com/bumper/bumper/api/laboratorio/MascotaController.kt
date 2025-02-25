package com.bumper.bumper.api

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/v1/mascota")
class MascotaController {

    @GetMapping
    fun retrieveHealth(): ResponseEntity<Mascota> {
        val mimascota = Mascota(tipo = "Perro", name = "Pelusa", peso = "6.8")

    return ResponseEntity.ok(mimascota)
    }

    @PostMapping
    fun createResourcia(@RequestBody mascotaBody: MascotaBody): ResponseEntity<Mascota> {

        val mimascota = Mascota(
            tipo = mascotaBody.tipo,
            name = mascotaBody.name,
            peso = mascotaBody.peso)
        return ResponseEntity.ok(mimascota)
    }
}