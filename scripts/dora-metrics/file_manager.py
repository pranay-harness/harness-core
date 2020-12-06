import csv

def init_csv_file_with_headers(filename, headers):
    with open(filename, 'w') as csvfile:
        # create new csv file and put headers in it
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the fields
        csvwriter.writerow(headers)


def create_new_file(filename):
    with open(filename, 'w') as file:
        pass


def append_to_csv_file(filename, data_rows):
    with open(filename, 'a') as csvfile:
        # creating a csv writer object
        csvwriter = csv.writer(csvfile)

        # writing the data rows
        csvwriter.writerows(data_rows)


def append_to_file(filename, data):
    with open(filename, 'a') as file:
        file.writelines(data)


def copy_csv_file(source_file, dest_file):
    with open(dest_file, 'a+') as w:
        writer = csv.writer(w)
        with open(source_file, 'r') as f:
            reader = csv.reader(f)
            for row in reader:
                writer.writerow(row)
