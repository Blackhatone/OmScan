# Plan de Implementación - Refinamiento de UI y Créditos

Mejorar la claridad de la interfaz mediante iconos estándar, organizar los ajustes y añadir los créditos del desarrollador.

## Cambios Propuestos

### 1. Iconos y Selección (Multiselección)

#### [MODIFICAR] [menu_selection.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/menu_selection.xml)
- Cambiar el icono de **Compartir** a un icono estándar de Android (`@android:drawable/ic_menu_share`).
- Cambiar el icono de **Eliminar** a un icono estándar de Android (`@android:drawable/ic_menu_delete`).

### 2. Organización de Ajustes

#### [MODIFICAR] [overflow.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/overflow.xml)
- Asegurar que la opción de **Ajustes** esté presente en el menú de desbordamiento (3 puntitos) de la barra superior.

#### [MODIFICAR] [navigation_drawer.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/navigation_drawer.xml) y [bottom_navigation.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/bottom_navigation.xml)
- Eliminar la entrada directa de **Ajustes** para simplificar la navegación principal.

### 3. Créditos del Desarrollador

#### [MODIFICAR] [bottom_navigation.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/bottom_navigation.xml) y [navigation_drawer.xml](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/res/menu/navigation_drawer.xml)
- Reemplazar el segundo botón (anteriormente Ajustes) con una nueva opción llamada **"Información"** o **"Autor"**.
- El icono será un icono de información (`@android:drawable/ic_dialog_info`).

#### [MODIFICAR] [MainActivity.kt](file:///home/santiago/AndroidStudioProjects/OmScan/app/src/main/java/com/example/omscan/MainActivity.kt)
- Manejar el clic en el nuevo botón para mostrar un mensaje (Toast o Diálogo): **"Desarrollado por @blackhatone"**.

## Plan de Verificación

### Pruebas Manuales
1.  **Iconos:** Activar el modo selección y verificar que los iconos de la barra superior sean claramente de "Compartir" y "Eliminar".
2.  **Ajustes:** Verificar que se puede acceder a Ajustes desde los 3 puntitos de la barra superior.
3.  **Créditos:** Tocar el segundo botón de la barra inferior/menú lateral y verificar que aparece el mensaje de crédito a @blackhatone.
