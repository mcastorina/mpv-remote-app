import setuptools

with open("README.md", "r") as fh:
    long_description = fh.read()

setuptools.setup(
    name="mpv_remote_app",
    version="0.0.5",
    author="miccah",
    author_email="m.castorina93@gmail.com",
    description="Server for the Android application",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/mcastorina/mpv-remote-app",
    packages=setuptools.find_packages(),
    python_requires='>=3',
    install_requires=[
        "psutil",
    ],
    classifiers=[
        "Programming Language :: Python :: 3",
        "License :: OSI Approved :: GNU General Public License v3 (GPLv3)",
        "Operating System :: OS Independent",
    ],
    entry_points={
        'console_scripts': [
            'mpv-remote-app=mpv_remote_app:main',
        ],
    },
)
