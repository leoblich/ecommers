import sys
import os
import time
from subprocess import Popen, PIPE
import pikepdf

def convert_word_to_pdf(input_file_path, output_dir, nombre):
    try:
        # Verificar si el archivo de entrada existe
        if not os.path.exists(input_file_path):
            raise FileNotFoundError(f"El archivo de entrada no existe: {input_file_path}")
        # Generar un nombre único para el archivo PDF

        pdf_filename = f"{nombre}_converted.pdf"
        pdf_path = os.path.join(output_dir, pdf_filename)

        # Verificar si el directorio de salida es válido
        if not os.path.isdir(output_dir):
            raise FileNotFoundError(f"El directorio de salida no existe: {output_dir}")

        # Ejecutar LibreOffice para convertir el archivo DOCX a PDF
        libreoffice_command = [
            "soffice",
            "--headless",
            "--convert-to", "pdf",
            "--outdir", output_dir,
            input_file_path
        ]
        process = Popen(libreoffice_command, stdout=PIPE, stderr=PIPE)
        stdout, stderr=process.communicate()

        # Renombrar el archivo PDF generado
        pdf_path_original = os.path.join(output_dir, os.path.basename(input_file_path).replace(".docx", ".pdf"))
        pdf_path_renamed = os.path.join(output_dir, pdf_filename)

        if process.returncode != 0:
            raise RuntimeError(f"Error durante la conversión: {stderr.decode().strip()}")

        # Verificar si el archivo PDF fue creado

        if not os.path.exists( pdf_path_original):
            raise FileNotFoundError(f"No se encontró el archivo PDF convertido: {pdf_path}")

        os.rename(pdf_path_original, pdf_path_renamed)  # Renombrar el archivo
        print(f"Conversión completada con éxito a {pdf_path}")

        try:
            # Abrir el archivo PDF
            with pikepdf.open(pdf_path) as pdf:
                num_pages = len(pdf.pages)  # Contar las páginas
            print(f"Número de páginas en el PDF: {num_pages}")
        except Exception as e:
            print(f"Error al contar las páginas: {e}")
            sys.exit(1)

        return pdf_path
    except Exception as e:
        print(f"Error durante la conversión: {e}", file=sys.stderr)
        sys.exit(1)

def extract_first_page(input_pdf, output_pdf):
    try:
        # Abrir el archivo PDF
        with pikepdf.open(input_pdf) as pdf:
            # Crear un nuevo archivo PDF para la primera página
            first_page_pdf = pikepdf.new()
            first_page_pdf.pages.append(pdf.pages[0])  # Añadir la primera página

            # Guardar la primera página en un nuevo archivo PDF
            first_page_pdf.save(output_pdf)

        print(f"Primera página extraída con éxito a {output_pdf}.")
    except Exception as e:
        print(f"Error al extraer la primera página: {e}")
        sys.exit(1)

if __name__ == "__main__":
    # Comprobar argumentos
    if len(sys.argv) < 4:
        print("Error: Se requieren tres argumentos: la ruta del archivo de entrada, el directorio de salida y el nombre del archivo.", file=sys.stderr)
        sys.exit(1)

    # Extracción de parámetros
    input_file_path = sys.argv[1]
    output_dir = sys.argv[2]
    nombre = sys.argv[3]

    # Ejecutar la conversión de Word a PDF
    pdf_file = convert_word_to_pdf(input_file_path, output_dir, nombre)

    # Generar la ruta del archivo para la primera página
    first_page_pdf_path = os.path.join(output_dir, f"{nombre}_first_page.pdf")

    # Extraer la primera página del PDF generado
    extract_first_page(pdf_file, first_page_pdf_path)

    # Añadir un retraso breve antes de intentar eliminar el archivo
    time.sleep(1)  # Espera un segundo para asegurar la liberación

    # Eliminar el archivo PDF original después de contar páginas y extraer la primera página
    try:
        os.remove(pdf_file)
        print(f"Archivo temporal {pdf_file} eliminado con éxito.")
    except Exception as e:
        print(f"Error al eliminar el archivo PDF: {e}", file=sys.stderr)