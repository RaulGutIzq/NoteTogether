class Usuarios {
    var id: Int = 0
    var nombre: String = ""
    var email: String = ""
    var password: String = ""

    // Método para clonar con modificaciones
    fun clone(
        id: Int = this.id,
        nombre: String = this.nombre,
        email: String = this.email,
        password: String = this.password
    ): Usuarios {
        val nuevoUsuario = Usuarios()
        nuevoUsuario.id = id
        nuevoUsuario.nombre = nombre
        nuevoUsuario.email = email
        nuevoUsuario.password = password
        return nuevoUsuario
    }
}