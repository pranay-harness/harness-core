import csv


def init_csv_file_with_headers(filepath, headers):
    with open(filepath, 'w') as csvfile:
        # create new csv file and put headers in it
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the fields
        csvwriter.writerow(headers)


def create_new_file(filepath):
    with open(filepath, 'w') as file:
        pass


def append_to_csv_file(filepath, data_rows):
    with open(filepath, 'a') as csvfile:
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the data rows
        csvwriter.writerows(data_rows)


def append_to_file(filepath, data):
    with open(filepath, 'a') as file:
        file.writelines(data)


def copy_csv_file(source_file_path, dest_file_path):
    with open(dest_file_path, 'a+') as w:
        writer = csv.writer(w)
        with open(source_file_path, 'r') as f:
            reader = csv.reader(f)
            for row in reader:
                writer.writerow(row)
